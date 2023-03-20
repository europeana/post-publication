package eu.europeana.postpublication.translation.service.impl;

import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.corelib.definitions.edm.beans.FullBean;
import eu.europeana.corelib.definitions.edm.entity.Proxy;
import eu.europeana.postpublication.translation.model.*;
import eu.europeana.postpublication.translation.service.LanguageDetectionService;
import eu.europeana.postpublication.translation.utils.LanguageDetectionUtils;
import eu.europeana.postpublication.translation.utils.TranslationUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.*;

@Service
public class RecordLangDetectionService {

    private static final Logger LOG = LogManager.getLogger(RecordLangDetectionService.class);

    // TODO check thgis field value in DB dctermsTableOfContents
    private static final Set<String> INCLUDE_PROXY_MAP_FIELDS = Set.of("dcContributor", "dcCoverage", "dcCreator", "dcDate", "dcDescription", "dcFormat", "dcIdentifier",
            "dcLanguage", "dcPublisher", "dcRelation", "dcRights", "dcSource", "dcSubject", "dcTitle", "dcType", "dctermsAlternative", "dctermsConformsTo", "dctermsCreated",
            "dctermsExtent", "dctermsHasFormat", "dctermsHasPart", "dctermsHasVersion", "dctermsIsFormatOf", "dctermsIsPartOf", "dctermsIsReferencedBy", "dctermsIsReplacedBy",
            "dctermsIsRequiredBy", "dctermsIssued", "dctermsIsVersionOf", "dctermsMedium", "dctermsProvenance", "dctermsReferences", "dctermsReplaces", "dctermsRequires",
            "dctermsSpatial", "dctermsTableOfContents", "dctermsTemporal", "edmCurrentLocation", "edmHasMet", "edmHasType", "edmIsRelatedTo", "edmType");

    private final LanguageDetectionService detectionService;

    /**
     * Create a new service for detecting Languages for the non-language tagged fields in non-europeana proxy fields
     *
     * @param detectionService underlying translation service to use for translations
     */
    public RecordLangDetectionService(LanguageDetectionService detectionService) {
        this.detectionService = detectionService;
    }


    /**
     * If does not match any of the languages Europeana supports or
     * if not supported by the language detection endpoint (ie. calling the isSupported method)
     * then there will be no hint supplied (this means that ‘mul’ is ignored)
     *
     * @param bean
     * @return
     */
    private String getHintForLanguageDetect(FullBean bean) {
        List<Language> edmLanguages = TranslationUtils.getEdmLanguage(bean);
        if (!edmLanguages.isEmpty()) {
            String edmLang = edmLanguages.get(0).name().toLowerCase(Locale.ROOT);
            if(detectionService.isSupported(edmLang)) {
                return edmLang;
            }
        }
        return null;
    }

    /**
     * Gather all non-language tagged values (for all whitelisted properties) of the (non-Europeana) Proxies
     * NOTE :: Only if there isn't a language tagged value already spelled exactly the same
     *
     * Run through language detection (ie. call lang detect method) and assign (or correct) language attributes for the values
     *
     * Responses indicating that the language is not supported or the inability to recognise the language should
     * retain the language attribute provided in the source
     *
     * Add all corrected language attributes to the Europeana Proxy (duplicating the value and assigning the new language attribute)
     *
     * @param bean
     * @throws EuropeanaApiException
     */
    public FullBean detectLanguageForProxy(FullBean bean) throws EuropeanaApiException {
        List<Proxy> proxies = new ArrayList<>(bean.getProxies()); // make sure we clone first so we can edit the list to our needs.

        // Data/santity check
        if (proxies.size() < 2) {
            LOG.error("Unexpected data - expected at least 2 proxies, but found only {}!", proxies.size());
            return bean;
        }
        String langHint = getHintForLanguageDetect(bean);

        // gather values from non-europeana proxy
        Proxy europeanaProxy = proxies.remove(0);
        if (!europeanaProxy.isEuropeanaProxy()) {
            LOG.error("Unexpected data - first proxy is not Europeana proxy!");
            return bean;
        }

        // 1. whitelisted properties filter
        ReflectionUtils.FieldFilter proxyFieldFilter = field -> field.getType().isAssignableFrom(Map.class) &&
                INCLUDE_PROXY_MAP_FIELDS.contains(field.getName());

        for (Proxy proxy : proxies) {
            // 2. gather all the "def" values for the whitelisted fields
            List<LanguageValueFieldMap> langValueFieldMapForDetection = new ArrayList<>();

            ReflectionUtils.doWithFields(proxy.getClass(), field -> {
                LanguageValueFieldMap fieldValuesLanguageMap = getProxyFieldsValues(proxy, field);
                if (fieldValuesLanguageMap != null) {
                    langValueFieldMapForDetection.add(fieldValuesLanguageMap);
                }

            }, proxyFieldFilter);
            LOG.debug("Gathered {} fields non-language tagged values for record {} and proxy {}", langValueFieldMapForDetection.size(), bean.getAbout(), proxy.getAbout());

            Map<String, Integer> textsPerField = new HashMap<>();
            List<String> textsForDetection = new ArrayList<>();

            // 3. collect all the values in one list for single lang-detection request per proxy
            LanguageDetectionUtils.getTextsForDetectionRequest(bean, textsForDetection, textsPerField, langValueFieldMapForDetection);

            // 4. send lang-detect request
            List<String> detectedLanguages= detectionService.detectLang(textsForDetection, langHint);
            LOG.debug("Detected languages - {} ", detectedLanguages);

            //5. assign language attributes to the values
            List<LanguageValueFieldMap> correctLangValueMap = LanguageDetectionUtils.getLangDetectedFieldValueMap(textsPerField, detectedLanguages, textsForDetection);

            // 6. add all the new language tagged values to europeana proxy
            Proxy europeanProxy = bean.getProxies().get(0);
            updateProxy(europeanProxy, correctLangValueMap); // add the new lang-value map for europeana proxy

        }
        return bean;
    }

    private LanguageValueFieldMap getProxyFieldsValues(Proxy proxy, Field field) {
         HashMap<String, List<String>> origFieldData = (HashMap<String, List<String>>) LanguageDetectionUtils.getValueOfTheField(proxy).apply(field.getName());
         return LanguageDetectionUtils.getValueFromLanguageMap(SerializationUtils.clone(origFieldData), field.getName());
    }

    /**
     * Updates the proxy object field values by adding the new map values
     * @param proxy
     * @param correctLangMap
     */
    private void updateProxy( Proxy proxy, List<LanguageValueFieldMap> correctLangMap) {
        correctLangMap.stream().forEach(value -> {
            Map<String, List<String>> map = LanguageDetectionUtils.getValueOfTheField(proxy).apply(value.getFieldName());

            //2. Now add the new lang-value map in the proxy
            for (Map.Entry<String, List<String>> entry : value.entrySet()) {
                if (map.containsKey(entry.getKey())) {
                    map.get(entry.getKey()).addAll(entry.getValue());
                } else {
                    map.put(entry.getKey(), entry.getValue());
                }
            }
        });
    }
}
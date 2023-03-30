package eu.europeana.postpublication.translation.service.impl;

import eu.europeana.api.commons.error.EuropeanaApiException;
import eu.europeana.corelib.definitions.edm.beans.FullBean;
import eu.europeana.corelib.definitions.edm.entity.Proxy;
import eu.europeana.postpublication.translation.model.*;
import eu.europeana.postpublication.translation.service.LanguageDetectionService;
import eu.europeana.postpublication.translation.utils.LanguageDetectionUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.*;

@Service
public class RecordLangDetectionService extends BaseRecordService {

    private static final Logger LOG = LogManager.getLogger(RecordLangDetectionService.class);

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
        List<Language> edmLanguages = LanguageDetectionUtils.getEdmLanguage(bean);
        if (!edmLanguages.isEmpty()) {
            String edmLang = edmLanguages.get(0).name().toLowerCase(Locale.ROOT);
            if (detectionService.isSupported(edmLang)) {
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

        // 1. gather all the "def" values for the whitelisted fields
        for (Proxy proxy : proxies) {
            List<LanguageValueFieldMap> langValueFieldMapForDetection = new ArrayList<>();

            ReflectionUtils.doWithFields(proxy.getClass(), field -> {
                LanguageValueFieldMap fieldValuesLanguageMap = getProxyFieldsValues(proxy, field, bean);
                if (fieldValuesLanguageMap != null) {
                    langValueFieldMapForDetection.add(fieldValuesLanguageMap);
                }

            }, proxyFieldFilter);

            LOG.debug("For record {} gathered {} fields non-language tagged values for detection. ", bean.getAbout(), langValueFieldMapForDetection.size());

            Map<String, Integer> textsPerField = new LinkedHashMap<>(); // to maintain the order of the fields
            List<String> textsForDetection = new ArrayList<>();

            // 3. collect all the values in one list for single lang-detection request per proxy
            LanguageDetectionUtils.getTextsForDetectionRequest(textsForDetection, textsPerField, langValueFieldMapForDetection);

            // 4. send lang-detect request
            List<String> detectedLanguages = detectionService.detectLang(textsForDetection, langHint);
            LOG.debug("Detected languages - {} ", detectedLanguages);
            //5. assign language attributes to the values
            List<LanguageValueFieldMap> correctLangValueMap = LanguageDetectionUtils.getLangDetectedFieldValueMap(textsPerField, detectedLanguages, textsForDetection);

            // 6. add all the new language tagged values to europeana proxy
            Proxy europeanProxy = bean.getProxies().get(0);
            updateProxy(europeanProxy, correctLangValueMap); // add the new lang-value map for europeana proxy
        }
        return bean;
    }

    private LanguageValueFieldMap getProxyFieldsValues(Proxy proxy, Field field, FullBean bean) {
         HashMap<String, List<String>> origFieldData = (HashMap<String, List<String>>) getValueOfTheField(proxy, false).apply(field.getName());
         return LanguageDetectionUtils.getValueFromLanguageMap(SerializationUtils.clone(origFieldData), field.getName(), bean);
    }

    /**
     * Updates the proxy object field values by adding the new map values
     * @param proxy
     * @param correctLangMap
     */
    private void updateProxy( Proxy proxy, List<LanguageValueFieldMap> correctLangMap) {
        correctLangMap.stream().forEach(value -> {
            Map<String, List<String>> map = getValueOfTheField(proxy, true).apply(value.getFieldName());

            // Now add the new lang-value map in the proxy
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
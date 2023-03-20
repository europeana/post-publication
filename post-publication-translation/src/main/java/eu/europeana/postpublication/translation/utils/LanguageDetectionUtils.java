package eu.europeana.postpublication.translation.utils;

import eu.europeana.corelib.definitions.edm.beans.FullBean;
import eu.europeana.corelib.definitions.edm.entity.ContextualClass;
import eu.europeana.corelib.definitions.edm.entity.Proxy;
import eu.europeana.corelib.utils.EuropeanaUriUtils;
import eu.europeana.postpublication.translation.model.Language;
import eu.europeana.postpublication.translation.model.LanguageValueFieldMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;

public class LanguageDetectionUtils {

    private static final Logger LOG = LogManager.getLogger(LanguageDetectionUtils.class);
    private static final List<String> ENTITIES = List.of("agents", "concepts", "places", "timespans");

    private LanguageDetectionUtils() {

    }

    /**
     * Function to get the lang-value map of the field from the proxy Object
     * @param proxy
     * @return
     */
    public static Function<String, Map<String, List<String>>> getValueOfTheField(Proxy proxy) {
        return e -> {
            Field field = ReflectionUtils.findField(proxy.getClass(), e);
            ReflectionUtils.makeAccessible(field);
            Object value = ReflectionUtils.getField(field, proxy);
            // if the field doesn't exist, set an empty map in the proxy object
            if (value == null) {
                ReflectionUtils.setField(field, proxy, new LinkedHashMap<>());
                value = ReflectionUtils.getField(field, proxy);
            }
            if (value instanceof Map) {
                return (Map<String, List<String>>) value;
            } else if (value != null) { // should not happen as the whitelisted values are all lang-map
                LOG.warn("Unexpected data - field {} did not return a map", e);
            }
            return new LinkedHashMap<>(); // default return an empty map
        };
    }

    /**
     * Method to get values of non-language tagged prefLabel (only if no other language tagged value doesn't exists)
     * @param entity entity object
     * @return
     */
    public static List<String> getPrefLabelofEntity(ContextualClass entity) {
        List<String> prefLabels = new ArrayList<>();
        if (entity != null) {
            Map<String, List<String>> map = entity.getPrefLabel();
            if (!map.isEmpty() && !map.keySet().isEmpty()) {
                // if preflabel is present in other languages than "def" then do nothing
                if (!map.isEmpty() && !map.keySet().isEmpty() && mapHasOtherLanguagesThanDef(map.keySet())) {
                    LOG.debug("Entity {} already has language tagged values", entity.getAbout());
                } else { // pick the def value
                    prefLabels.addAll(map.get(Language.DEF));
                }
            }
        }
        return prefLabels;
    }

    /**
     * Finds the Contextual entity from the bean matching the uri
     * @param bean record
     * @param uri url to check
     * @return
     */
    public static ContextualClass entityExistsWithUrl(FullBean bean, String uri) {
        List<ContextualClass> matchingEntity= new ArrayList<>();

        // check only entity objects
        ReflectionUtils.FieldFilter entityFilter = field -> ENTITIES.contains(field.getName());

        ReflectionUtils.doWithFields(bean.getClass(), field -> {
            // if we found the Contextual class already, no need to iterate more
            if (matchingEntity.size() == 1) {
                return;
            }
            ReflectionUtils.makeAccessible(field);
            Object o = ReflectionUtils.getField(field, bean);
            LOG.trace("Searching for entities with type {}...", field.getName());
            // check only if it's a list and is not empty
            if (o instanceof List && !((List<?>) o).isEmpty()) {
                    List<ContextualClass> entities = (List<ContextualClass>) o;
                    for (ContextualClass entity : entities) {
                        if (StringUtils.equalsIgnoreCase(uri, entity.getAbout())) {
                            LOG.debug("  Found matching entity {}", entity.getAbout());
                            matchingEntity.add(entity);
                            break;
                        }
                    }
            }
        }, entityFilter);

        // return Contextual Class if found or else null
        return matchingEntity.isEmpty() ? null : matchingEntity.get(0);
    }


    /**
     * This methods adds the texts to be sent for detection in a list.
     * Additionally also saves the texts sent per field for detection
     * If the text is a uri -
     *          checks if contextual entity exists -
     *                YES - Check if preflabel ONLY in "def" is present and add that value for detection
     *                NO - then add that uri value for lang-detection
     *
     * @param bean Full bean record
     * @param textsForDetection List to store texts to be sent for language detection
     * @param textsPerField to add the text size sent for detection per field
     * @param langValueFieldMapForDetection lang-value "def" map for the whitelisted field
     */
    public static void getTextsForDetectionRequest(FullBean bean, List<String> textsForDetection,
                                                   Map<String, Integer> textsPerField,List<LanguageValueFieldMap> langValueFieldMapForDetection ) {
        for(LanguageValueFieldMap languageValueFieldMap : langValueFieldMapForDetection) {
            int textPerField = 0;
            for (Map.Entry<String, List<String>> def : languageValueFieldMap.entrySet()) {
                for (String value : def.getValue()) {
                    if (EuropeanaUriUtils.isUri(value)) {
                        ContextualClass entity = entityExistsWithUrl(bean, value);
                        if (entity != null) {
                            // preflabels here will either have "def" values (only if there was no other language value present) OR will be empty
                            List<String> preflabels = getPrefLabelofEntity(entity);
                            textsForDetection.addAll(preflabels);
                            textPerField += preflabels.size();
                        } else {
                            textsForDetection.add(value); // add the uri whose contextual entity doesn't exist
                            textPerField++;
                        }
                    } else {
                        textsForDetection.add(value); // add other texts as it is
                        textPerField++;
                    }
                }
                textsPerField.put(languageValueFieldMap.getFieldName(), textPerField);
            }
        }
    }

    /**
     * Assigns the correct language to the values for the fields
     * @param textsPerField number of texts present per field list
     * @param detectedLanguages languages detected by the Engine
     * @param textsForDetection texts sent for lang-detect requests
     * @return
     */
    public static List<LanguageValueFieldMap> getLangDetectedFieldValueMap(Map<String, Integer> textsPerField, List<String> detectedLanguages, List<String> textsForDetection) {
        int counter =0;
        List<LanguageValueFieldMap> correctLangMap = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : textsPerField.entrySet()) { // field loop
            // new value map for field
            Map<String, List<String>> newValueMap = new HashMap<>();

            for(int i=0; i< entry.getValue(); i++) {
                String newLang = detectedLanguages.get(counter);
                // if the service did not return any language for the text then source language should be kept intact
                // which is "def" in these cases
                newLang = newLang == null ? Language.DEF : newLang;
                if (newValueMap.containsKey(newLang)) {
                    newValueMap.get(newLang).add(textsForDetection.get(counter));
                } else {
                    List<String> values = new ArrayList<>();
                    values.add(textsForDetection.get(counter));
                    newValueMap.put(detectedLanguages.get(counter), values);
                }
                counter++;
            }
            // add the new map for the field
            correctLangMap.add(new LanguageValueFieldMap(entry.getKey(), newValueMap));
        }
        return correctLangMap;
    }

    /**
     * Returns the def values of the field (removing the values which are already present in the lang-tagged)
     *
     * @param map map of the field
     * @param fieldName field name
     * @return
     */
    public static LanguageValueFieldMap getValueFromLanguageMap(Map<String, List<String>> map, String fieldName) {
        // get non-language tagged values only
        if (!map.keySet().isEmpty() && map.containsKey(Language.DEF)) {
            List<String> values = map.get(Language.DEF);
            // check if there is if there is any other language present in the map and
            // if yes, then check if lang-tagged values already have the def tagged values present
            if (LanguageDetectionUtils.mapHasOtherLanguagesThanDef(map.keySet())) {
                List<String> defValuesNow = LanguageDetectionUtils.removeLangTaggedValuesFromDef(map);
                return new LanguageValueFieldMap(fieldName, Language.DEF, defValuesNow);
            } else {
                return new LanguageValueFieldMap(fieldName, Language.DEF, values);
            }
        }
        return null;
    }


    /**
     * Checks if map contains keys other than "def"
     * @param keyset
     * @return
     */
    public static boolean mapHasOtherLanguagesThanDef(Set<String> keyset) {
        Set<String> copy = new HashSet<>(keyset); // deep copy
        copy.remove(Language.DEF);
        return !copy.isEmpty();
    }

    /**
     * Remove the lang-tagged values from "def"
     *
     * ex if map has values : {def=["paris", "budapest" , "venice"], en=["budapest"]}
     * then returns : ["paris", "venice"]
     * @param map
     * @return
     */
    public static List<String> removeLangTaggedValuesFromDef(Map<String, List<String>> map) {
        List<String> nonLangTaggedDefvalues = new ArrayList<>(map.get(Language.DEF));
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            if (!entry.getKey().equals(Language.DEF)) {
                nonLangTaggedDefvalues.removeAll(entry.getValue());
            }
        }
        return nonLangTaggedDefvalues;
    }
}

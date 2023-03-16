package eu.europeana.postpublication.translation.utils;

import eu.europeana.corelib.definitions.edm.beans.FullBean;
import eu.europeana.corelib.utils.ComparatorUtils;
import eu.europeana.postpublication.translation.exception.TranslationException;
import eu.europeana.postpublication.translation.model.Language;
import eu.europeana.postpublication.translation.model.FieldValuesLanguageMap;
import eu.europeana.postpublication.translation.service.TranslationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Helper class for sending translation request
 */
public final class TranslationUtils {

    public static final String FIELD_NAME_INDEX_SEPARATOR = ".";
    public static final String FIELD_NAME_INDEX_REGEX = "\\.";

    private static final Logger LOG = LogManager.getLogger(TranslationUtils.class);

    private TranslationUtils() {
        // empty constructor to prevent initialization
    }

    /**
     * Gathers all texts (values) present in a TranslationMap and puts these in a single query to the translation
     * service. When the results are returned, all translation parts are put back in a similar appropriate map using the
     * same field names (keys).
     * @param translationService the translation service to use
     * @param toTranslate map consisting of field names and texts to translate
     * @param targetLanguage the language to translate to
     * @return map with the same field names, but now mapped to translated values
     * @throws eu.europeana.postpublication.translation.exception.TranslationException when there is a problem sending/retrieving translation data
     */
    public static FieldValuesLanguageMap translate(TranslationService translationService, FieldValuesLanguageMap toTranslate,
                                                   String targetLanguage, String langHint) throws TranslationException {
        // We don't use delimiters because we want to keep the number of characters we sent low.
        // Instead we use line counts to determine start and end of a field.
        Map<String, Integer> linesPerField = new LinkedHashMap<>();
        List<String> linesToTranslate = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : toTranslate.entrySet()) {
            List<String> lines = entry.getValue();
            linesPerField.put(entry.getKey(), lines.size());
            linesToTranslate.addAll(lines);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Key {}, values to translate {}", entry.getKey(), lines);
            }
        }

        // Actual translation
        List<String> translations;
        try {
            if (Language.DEF.equals(toTranslate.getSourceLanguage())) {
                LOG.debug("Sending translate query with language detect...");
                translations = translationService.translateAndDetect(linesToTranslate, targetLanguage, langHint);
            } else {
                LOG.debug("Sending translate query with source language {} and target language {}...", toTranslate.getSourceLanguage(), targetLanguage);
                translations = translationService.translate(linesToTranslate, targetLanguage, toTranslate.getSourceLanguage());
            }
        }
        // TODO this is not needed anymore as we are not doing Gog;e translation but identify what other exceptions can be thrown
//        catch (ResourceExhaustedException e) {
//            // catch Google StatusRuntimeException: RESOURCE_EXHAUSTED exception
//            // this will be thrown if the limit for the day is exceeded
//            throw new TranslationServiceLimitException(e);
//        }
        catch (RuntimeException e) {
            // Catch Google Translate issues and wrap in our own exception
            throw new TranslationException(e.getMessage());
        }
        // Sanity check
        if (translations.size() != linesToTranslate.size()) {
            throw new IllegalStateException("Expected " + linesToTranslate.size() + " lines of translated text, but received " + translations.size());
        }

        // Put received data under appropriate key in new map
        FieldValuesLanguageMap result = new FieldValuesLanguageMap(targetLanguage);
        int counter = 0;
        for (String key : toTranslate.keySet()) {
            int nrLines = linesPerField.get(key);
            if (nrLines == 0) {
                result.put(key, null);
            } else {
                result.put(key, translations.subList(counter, counter + nrLines));
            }
            counter = counter + nrLines;
        }
        return result;
    }

    /**
     * Removes the values from the translated Map,
     * If values in original map are equal to the values in translated map
     * NOTE : Comparison is made for each value ignoring spaces and case.
     *        Values which actually are changed or translated are finally added in the map.
     *
     * @param translatedMap map containing the fields and translated values
     * @param mapToTranslate map containing the fields and original values to be translated
     * @Returns map containing fields and actual-translated values
     */
    public static FieldValuesLanguageMap removeIfOriginalIsSameAsTranslated(FieldValuesLanguageMap translatedMap, FieldValuesLanguageMap mapToTranslate) {
        Map<String , List<String>> actualTranslationMap = new HashMap<>();
        for (Map.Entry<String, List<String>> translated : translatedMap.entrySet()) {
            List<String> actualTranslatedValues = new ArrayList<>();
            // we have already performed the sanity check in translate() method. So the list size will be equal here
            for (int i = 0; i < translated.getValue().size(); i++) {
                String origValue = mapToTranslate.get(translated.getKey()).get(i);
                if (!ComparatorUtils.sameValueWithoutSpace(origValue, translated.getValue().get(i))) {
                    actualTranslatedValues.add(translated.getValue().get(i));
                } else {
                    LOG.debug("Skipping translated def " +
                            "value as it's the same as original: {}", origValue);
                }
            }
            if (!actualTranslatedValues.isEmpty()) {
                actualTranslationMap.put(translated.getKey(), actualTranslatedValues);
            }
        }
        if (!actualTranslationMap.isEmpty()) {
            return new FieldValuesLanguageMap(translatedMap.getSourceLanguage(), actualTranslationMap);
        }
        return null;
    }

    /**
     * Given a particular language code, this retrieves a value from a language map
     * Note that the language in the map might be present with or without region codes e.g. "en-GB".
     * So we first try an exact match with the provided 2-letter language code, and if that returns nothing we try
     * a partial starts-with match.
     *
     * @param map
     * @param lang
     * @return null if nothing was found
     */
    public static List<String> getValuesToTranslateFromMultilingualMap(Map<String, List<String>> map, String lang) {
        List<String> valuesToTranslate = map.get(lang);
        if (valuesToTranslate == null || valuesToTranslate.isEmpty()) {
            valuesToTranslate = map.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith(lang))
                    .map(Map.Entry :: getValue)
                    .findFirst().orElse(null);
        }
        return valuesToTranslate;
    }

    /**
     * Add translations to the provided field of the provided object. The field must be a Language map
     * @param object object to which translations are added
     * @param fieldName name of the field part of the object to which translations are added
     * @param lang the language in which translations are (used as key in the language map)
     * @param values the translated values to add
     */
    public static void addTranslationsToObject(Object object, String fieldName, String lang, List<String> values) {
        Field field = ReflectionUtils.findField(object.getClass(), fieldName);
        if (field == null) {
            LOG.error("Cannot find field with name {} in object {}", fieldName, object.getClass().getSimpleName());
        } else {
            ReflectionUtils.makeAccessible(field);
            Object o = ReflectionUtils.getField(field, object);
            if (o instanceof Map) {
                Map<String, List<String>> map = (Map<String, List<String>>) o;
                if (map.containsKey(lang)) {
                    // should not happen!
                    LOG.error("Object {} already has values for field {} and language {}!", object.getClass(), fieldName, lang);
                } else {
                    LOG.trace("Adding to field {}, key {} and value {}", fieldName, lang, values);
                    map.put(lang, values);
                }
            } else {
                Map<String, List<String>> newMap = new LinkedHashMap<>();
                newMap.put(lang, values);
                ReflectionUtils.setField(field, object, newMap);
            }
        }
    }

    /**
     * Add translations to the provided field of the provided list. The field must be a Language map and the provided
     * fieldname must start with the list index followed by a dot. For example field <pre>0.dcCreatorLangAware</pre>
     * refers to the field named dcCreatorLangAware in the first item in the list
     * We also need to convert back to the proper Solr names, since these will be filtered later while presenting the
     * final results (using EmdUtils.cloneMap)
     * @param list list containing objects to which translations are added
     * @param fieldName index of the object in the list and name of the field to which translations are added
     * @param solrKeyName, the name of the key as used in solr (e.g. proxy_dc_creator.en)
     * @param values the translated values to add
     */
    public static void addTranslationsToList(List list, String fieldName, String solrKeyName, List<String> values) {
        try {
            LOG.trace("   add fieldName = {}, solrKeyname = {}, values = {}", fieldName, solrKeyName, values);
            String[] parts = fieldName.split(FIELD_NAME_INDEX_REGEX);
            int index = Integer.valueOf(parts[0]);
            String fName = parts[1];
            addTranslationsToObject(list.get(index), fName, solrKeyName, values);
        } catch (RuntimeException e) {
            LOG.warn("Error reading fieldName {}. Unable to add translation for it.", fieldName, e);
        }
    }


    /**
     * Returns the default language list of the edm:languages
     * NOTE : For region locales values, if present in edm:languages
     * the first two ISO letters will be picked up.
     * <p>
     * Only returns the supported official languages,See: {@link Language}
     * Default translation and filtering for non-official language
     * is not supported
     *
     * @param bean the fullbean to inspect
     * @return the default language as specified in Europeana Aggregation edmLanguage field (if the language found there
     * is one of the EU languages we support in this application for translation)
     */
    public static List<Language> getEdmLanguage(FullBean bean) {
        List<Language> lang = new ArrayList<>();
        Map<String, List<String>> edmLanguage = bean.getEuropeanaAggregation().getEdmLanguage();
        for (Map.Entry<String, List<String>> entry : edmLanguage.entrySet()) {
            for (String languageAbbreviation : entry.getValue()) {
                if (Language.isSupported(languageAbbreviation)) {
                    lang.add(Language.getLanguage(languageAbbreviation));
                } else {
                    LOG.warn("edm:language '{}' is not supported", languageAbbreviation);
                }
            }
        }
        if (!lang.isEmpty()) {
            LOG.debug("Default translation and filtering applied for language : {} ", lang);
        }
        return lang;
    }



}

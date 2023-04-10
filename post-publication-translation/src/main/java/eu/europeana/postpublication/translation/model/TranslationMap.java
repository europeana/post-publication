package eu.europeana.postpublication.translation.model;

import eu.europeana.api.commons.definitions.utils.ComparatorUtils;
import eu.europeana.postpublication.translation.exception.TranslationException;
import eu.europeana.postpublication.translation.service.TranslationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.*;

public class TranslationMap extends LinkedHashMap<String, List<String>> {

    private static final long serialVersionUID = 7857857025275959529L;

    private static final Logger LOG = LogManager.getLogger(TranslationMap.class);

    @Nonnull
    private final String sourceLanguage;


    public TranslationMap(@Nonnull String sourceLanguage) {
        this.sourceLanguage = sourceLanguage;
    }

    public TranslationMap(@Nonnull String sourceLanguage, String fieldName, List<String> values) {
        this.sourceLanguage = sourceLanguage;
        add(fieldName, values);
    }

    /**
     * Adds the fieldname and the list of values for that field in the Translation map
     * @param fieldName
     * @param values
     */
    public void add(String fieldName, List<String> values) {
        if( fieldName != null && !values.isEmpty()) {
            if(this.containsKey(fieldName)) {
                this.get(fieldName).addAll(values);
            } else {
                this.put(fieldName, values);
            }
        }
    }

    /**
     * Translates the field value map using the translation service provided in the target Language
     * We know already with the translation workflow, there is only one source language (chosen language)
     * in which all the data for the fields is gathered
     *
     * Logging pattern for ELK : rid:<record_id> <success_count> <discarded_count> <failed_count> <fieldId> <lang_code>
     * Failed scenarios only happen when there are no translations returned by the service
     * for Discarded scenarios (values below threshold), null values are returned to the translations
     *
     * @param translationService service for the translation
     * @param targetLanguage language in which values are to be translated
     * @return translation map with target language and translations
     * @throws TranslationException
     */
    public TranslationMap translate(TranslationService translationService, String targetLanguage, String recordId) throws TranslationException {

        // save the field name and size per field (number of values associated with it)
        // to retain the order using LinkedHashmap
        Map<String, Integer> textsPerField = new LinkedHashMap<>();
        try {
            // add all the texts from the map to send a single request
            List<String> textsToTranslate = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : this.entrySet()) {
                textsToTranslate.addAll(entry.getValue());
                textsPerField.put(entry.getKey(), entry.getValue().size());
            }

            // send request for translation
            LOG.debug("Sending translate request with target language - {} and source language - {}", targetLanguage, this.sourceLanguage);
            List<String> translations = translationService.translate(textsToTranslate, targetLanguage, this.sourceLanguage, false);

            //fail safe check
            if (translations.size() != textsToTranslate.size()) {
                throw new IllegalStateException("Expected " + textsToTranslate.size() + " lines of translated text, but received " + translations.size());
            }

            // create the target language - translated map from the translations received from the service
            TranslationMap translatedMap = new TranslationMap(targetLanguage);
            int fromIndex = 0;
            for (Map.Entry<String, Integer> entry : textsPerField.entrySet()) {
                int toIndex = fromIndex + entry.getValue();

                // get the translation values for the field. We do not want to modify translation list hence deep copy
                List<String> values = new ArrayList<>();
                values.addAll(translations.subList(fromIndex, toIndex));
                translatedMap.add(entry.getKey(), getTranslationsToAdd(values, entry.getValue(), entry.getKey(), recordId));
                fromIndex += entry.getValue();
            }

            return translatedMap;
        } catch (TranslationException e) {
            // Only if no translations were returned, Logging the failed scenarios
            for (Map.Entry<String, Integer> entry : textsPerField.entrySet()) {
                LOG.info("rid: {} {} {} {} {} {} ", recordId, 0, 0, entry.getValue(), entry.getKey(), this.sourceLanguage);
            }
            throw new TranslationException(e.getMessage());
        }
    }

    /**
     * Returns translations for the specific field after removing null and duplicates
     * @param translationsForField translations for that field
     * @param textPerFieldSize original texts size sent for translation
     * @param fieldName field name
     * @param recordId record Id
     * @return
     */
    private List<String> getTranslationsToAdd(List<String> translationsForField, Integer textPerFieldSize, String fieldName, String recordId) {
            Integer success =0;
            Integer discarded = 0;
            // remove null values for discarded translations due to lower thresholds
            boolean haveDiscarded = translationsForField.removeIf(Objects::isNull);
            if (haveDiscarded) {
                discarded = textPerFieldSize - translationsForField.size();
            }
            success = translationsForField.size();
            // remove duplicates per field
            ComparatorUtils.removeDuplicates(translationsForField);

            LOG.info("rid: {} {} {} {} {} {} ", recordId, success, discarded, textPerFieldSize - (success+discarded), fieldName, this.sourceLanguage);
            return translationsForField;
    }

    @Nonnull
    public String getSourceLanguage() {
        return sourceLanguage;
    }
}

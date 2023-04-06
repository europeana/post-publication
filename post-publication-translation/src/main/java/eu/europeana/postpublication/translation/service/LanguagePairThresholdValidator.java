package eu.europeana.postpublication.translation.service;

import eu.europeana.postpublication.translation.model.LanguagePair;

import java.util.Map;
import java.util.function.Function;

/**
 * Abstract class to validate the score obtained from the translation service
 * for the valid Language Pair.
 *
 * Note : this is mainly for future Translation API when we will have more than one translation service
 *        and their own valid languages or pairs for translations
 *
 * @author Srishti Singh
 * @since 6 April 2023
 */
public abstract class LanguagePairThresholdValidator {

    private LanguagePairThresholdValidator() {
    }

    /**
     * For the given language pair and score value returns true is the score greater than the acceptable threshold
     * @param langPairThresholdMap LanguagePair and threshold map for the translation service.
     *                             Contains the valid language pairs and their acceptable threshold values
     * @param score score value obtained for the translation from the service
     * @return true if score > threshold value obtained from the langPairThresholdMap map for the given Language pair
     */
    public static Function<LanguagePair, Boolean> isScoreAcceptable(Map<LanguagePair, Double> langPairThresholdMap, double score) {
        return languagePair -> {
            if (langPairThresholdMap.containsKey(languagePair)) {
                Double acceptableThreshold = langPairThresholdMap.get(languagePair);
                if (score > acceptableThreshold) {
                    return true;
                }
            }
            return false;
        };
    }
}

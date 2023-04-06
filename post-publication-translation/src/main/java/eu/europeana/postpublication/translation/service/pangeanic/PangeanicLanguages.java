package eu.europeana.postpublication.translation.service.pangeanic;

import eu.europeana.postpublication.translation.model.Language;
import eu.europeana.postpublication.translation.model.LanguagePair;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Enum class for Pangeanic supported Languages and the threshold values for the languages during translations
 *
 * @author Srishti Singh
 * @since 6 April 2023
 */
public enum PangeanicLanguages {

    SK(87.40), RO(85.45), BG(86.78), PL(87.10), HR(89.01),
    SV(78.50), FR(86.50), IT(82.46), ES(82.13), CS(84.10),
    DE(82.88), LV(78.84), NL(77.57), EL(80.09), FI(79.75),
    DA(75.48), SL(75.14), HU(77.79), PT(72.43), ET(74.24),
    LT(65.38), GA(86.50);

    private final double translationThresholds;

    protected static final Logger LOG = LogManager.getLogger(PangeanicLanguages.class);

    PangeanicLanguages(double translationThresholds) {
        this.translationThresholds = translationThresholds;
    }

    public double getTranslationThresholds() {
        return translationThresholds;
    }

    private static final Set<String> SUPPORTED_LANGUAGES = new HashSet<>(Stream.of(PangeanicLanguages.values())
            .map(Enum::name)
            .collect(Collectors.toList()));

    private static final List<LanguagePair> TRANSLATION_PAIRS = new ArrayList<>(Stream.of(PangeanicLanguages.values())
            .map(e -> new LanguagePair(e.name(), Language.EN.name()))
            .collect(Collectors.toList()));

    /**
     * Returns the threshold value for the Language.
     *
     * @param lang
     * @return
     */
    public static double getThresholdForLanguage(String lang) {
        for (PangeanicLanguages e : values()) {
            if (StringUtils.equalsIgnoreCase(e.name(), lang)) {
                return e.getTranslationThresholds();
            }
        }
        return 0.0;
    }

    /**
     * Returns true is the provided language is supported by Pangeanic
     *
     * @param language
     * @return
     */
    public static boolean isLanguageSupported(String language) {
        return SUPPORTED_LANGUAGES.contains(language.toUpperCase(Locale.ROOT));
    }


    /**
     * Returns true if Language pair is supported for Pangeanic Translation
     *
     * @param srourceLang source langauge
     * @param targetLang  target lanaguge. Always "en" in the case of Pangeanic
     * @return
     */
    public static boolean isLanguagePairSupported(String srourceLang, String targetLang) {
        if (!StringUtils.equals(targetLang, Language.ENGLISH)) {
            LOG.error("For Pangeanic Translations target language must always be 'en' - {}" , targetLang);
            return false;
        }
        return TRANSLATION_PAIRS.contains(new LanguagePair(srourceLang.toUpperCase(Locale.ROOT), targetLang.toUpperCase(Locale.ROOT)));
    }
}

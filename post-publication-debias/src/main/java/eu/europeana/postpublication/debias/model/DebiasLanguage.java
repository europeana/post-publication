package eu.europeana.postpublication.debias.model;

import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Debias language class for the supported languages
 * The languages that the tool will support are: French: fr, German: de, Dutch: nl, Italian: it, English: en
 * @author Srishti Singh
 */
public enum DebiasLanguage {

    EN, NL, FR, DE, IT ;

    private static final Set<String> LANGUAGES = new HashSet<>(Stream.of(DebiasLanguage.values())
            .map(Enum::name)
            .toList());

    private static final String SEPARATOR = ",";

    public static final String DEF = "def";
    public static final String NO_LINGUISTIC_CONTENT = "zxx";
    public static final String ENGLISH = DebiasLanguage.EN.name().toLowerCase(Locale.ROOT);


    public static DebiasLanguage getLanguage(String lang) {
        return DebiasLanguage.valueOf(stripLangStringIfRegionPresent(lang).toUpperCase(Locale.ROOT));
    }

    /**
     * Check if a particular string is one of the supported languages
     * @param lang 2 letter ISO-code abbrevation of a language
     * @return true if we support it, otherwise false
     */
    public static boolean isSupported(String lang) {
        return LANGUAGES.contains(stripLangStringIfRegionPresent(lang).toUpperCase(Locale.ROOT));
    }

    /**
     * Check if the provided language code indicates no linguistic content
     * (see also https://en.wikipedia.org/wiki/Zxx)
     * @param lang language code to check
     * @return true if provided language is zxx, else false
     */
    public static boolean isNoLinguisticContent(String lang) {
        return NO_LINGUISTIC_CONTENT.equalsIgnoreCase(lang);
    }

    /**
     * Return true, if lang value is with regions ex: en-GB
     * @param lang
     * @return
     */
    private static boolean isLanguageWithRegionLocales(String lang) {
        return lang.length() > 2 && lang.contains("-") ;
    }

    /**
     * returns the substring  before '-' if lang value is with region locales
     * @param lang
     * @return
     */
    private static String stripLangStringIfRegionPresent(String lang) {
        if (isLanguageWithRegionLocales(lang)) {
            return StringUtils.substringBefore(lang, "-");
        }
        return lang;
    }
}

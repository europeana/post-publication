package eu.europeana.postpublication.translation.service;

import eu.europeana.postpublication.translation.exception.TranslationException;
import java.util.List;

/**
 * Generic translation service interface
 */
public interface TranslationService {


    /**
     * To validate the given pair of source and target language is valid for translation
     * @param srcLang source language of the data to be translated
     * @param trgLang target language in which data has to be translated
     * @return true is the pair is valid
     */
    boolean isSupported(String srcLang, String trgLang);

    /**
     * To fetch the source language for the list of texts.
     * If passed, langHint is used a hint in the method
     * @param texts to detect source language
     * @param langHint optional, hint to identify source language in which the texts are available
     * @return
     * @throws TranslationException
     */

    List<String> detectLang(List<String> texts, String langHint)
            throws TranslationException;


    /**
     * Translate multiple texts
     * @param texts to translate
     * @param targetLanguage language into which the texts are translated
     * @param sourceLanguage source lanaguge of the texts to be translated
     * @return translations of the provided texts
     * @throws TranslationException when there is a problem sending the translation request
     */
    List<String> translate(List<String> texts, String targetLanguage, String sourceLanguage) throws TranslationException;


    /**
     * Translate and detect the source language of multiple texts
     * @param texts to translate
     * @param targetLanguage language into which the texts are translated
     * @param langHint optional, hint to identify source language in which the texts are available
     * @return translations of the provided texts
     * @throws TranslationException
     */
    List<String> translateAndDetect(List<String> texts, String targetLanguage, String langHint)
            throws TranslationException;

    /**
     * To close Translation Engine
     */
    void close() ;

}


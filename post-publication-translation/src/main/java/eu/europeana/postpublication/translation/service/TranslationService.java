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
     * Translate multiple texts
     * @param texts to translate
     * @param targetLanguage language into which the texts are translated
     * @param sourceLanguage source lanaguge of the texts to be translated
     * @return translations of the provided texts
     * @throws TranslationException when there is a problem sending the translation request
     */
    List<String> translate(List<String> texts, String targetLanguage, String sourceLanguage, boolean detect) throws TranslationException;

    /**
     * to close the engine
     */
    void close();

}


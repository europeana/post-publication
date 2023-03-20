package eu.europeana.postpublication.translation.service;

import eu.europeana.postpublication.translation.exception.TranslationException;

import java.util.List;

public interface LanguageDetectionService {

    boolean isSupported(String srcLang);

    /**
     * To fetch the source language for the list of texts.
     * If passed, langHint is used a hint in the method
     * @param texts to detect source language
     * @param langHint optional, hint to identify source language in which the texts are available
     * @return
     * @throws TranslationException
     */

    List<String> detectLang(List<String> texts, String langHint) throws TranslationException;

    /**
     * to close the engine
     */
    void close();

}

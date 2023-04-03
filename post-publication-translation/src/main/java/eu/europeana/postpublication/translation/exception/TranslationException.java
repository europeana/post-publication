package eu.europeana.postpublication.translation.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

/**
 * Exception that is thrown when there is an error using the translation service
 */
public class TranslationException extends EuropeanaApiException {

    public TranslationException(String msg) {
        super(msg);
    }

    @Override
    public HttpStatus getResponseStatus() {
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
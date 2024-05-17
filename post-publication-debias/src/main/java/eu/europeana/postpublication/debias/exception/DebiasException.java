package eu.europeana.postpublication.debias.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

public class DebiasException extends EuropeanaApiException {

    public DebiasException(String msg) {
        super(msg);
    }

    public DebiasException(String msg, Throwable t) {
        super(msg, t);
    }

    @Override
    public HttpStatus getResponseStatus() {
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
package eu.europeana.postpublication.exception;

import eu.europeana.api.commons.error.EuropeanaApiException;
import org.springframework.http.HttpStatus;

public class InvalidExecutionStep extends EuropeanaApiException {

    public InvalidExecutionStep(String msg) {
        super(msg);
    }

    @Override
    public HttpStatus getResponseStatus() {
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
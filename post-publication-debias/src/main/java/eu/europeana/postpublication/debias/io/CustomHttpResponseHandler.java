package eu.europeana.postpublication.debias.io;

import eu.europeana.postpublication.debias.model.DebiasResponse;
import eu.europeana.postpublication.debias.service.BaseService;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.stanbol.commons.exception.JsonParseException;

import java.io.IOException;

public class CustomHttpResponseHandler extends BaseService implements HttpClientResponseHandler<DebiasResponse> {

    @Override
    public DebiasResponse handleResponse(ClassicHttpResponse classicHttpResponse) throws HttpException, IOException {

        int httpStatusCode = classicHttpResponse.getCode();
        if (httpStatusCode != HttpStatus.SC_OK) {
            throw new IOException("Error from Debias API: " +
                    httpStatusCode + " - " + classicHttpResponse.getReasonPhrase());
        } else {
            try {
                String json = EntityUtils.toString(classicHttpResponse.getEntity());
                return deserialize(json);
            } catch (JsonParseException e) {
                throw new IOException("Error deserializing response");
            }
        }
    }

}

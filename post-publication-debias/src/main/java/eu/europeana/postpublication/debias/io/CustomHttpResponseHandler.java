package eu.europeana.postpublication.debias.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.annotation.definitions.model.Annotation;
import eu.europeana.postpublication.debias.model.DebiasResponse;
import eu.europeana.postpublication.debias.utils.SerialisationUtils;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.stanbol.commons.exception.JsonParseException;

import java.io.IOException;
import java.util.List;

public class CustomHttpResponseHandler extends SerialisationUtils implements HttpClientResponseHandler<List<Annotation>> {

    private ObjectMapper mapper;

    public CustomHttpResponseHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<Annotation> handleResponse(ClassicHttpResponse classicHttpResponse) throws HttpException, IOException {

        int httpStatusCode = classicHttpResponse.getCode();
        if (httpStatusCode != HttpStatus.SC_OK) {
            throw new IOException("Error from Debias API: " +
                    httpStatusCode + " - " + classicHttpResponse.getReasonPhrase());
        } else {
            try {
                String json = EntityUtils.toString(classicHttpResponse.getEntity());
                System.out.println(json);
                return deserialize(mapper, json);
            } catch (JsonParseException e) {
                throw new IOException("Error from AnnotationLdParser while deserializing response  - " + e.getMessage(), e);
            }
        }
    }
}

package eu.europeana.postpublication.debias.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import eu.europeana.annotation.definitions.model.Annotation;
import eu.europeana.postpublication.debias.exception.DebiasException;
import eu.europeana.postpublication.debias.io.ContextSerializer;
import eu.europeana.postpublication.debias.model.Context;
import eu.europeana.postpublication.debias.model.DebiasRequest;
import eu.europeana.postpublication.debias.utils.SerialisationUtils;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.stanbol.commons.exception.JsonParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

/**
 * Debias service to send request and fetch list of annotations
 * @author Srishti Singh
 */
@PropertySource("classpath:post-publication.properties")
@PropertySource(value = "classpath:post-publication.user.properties", ignoreResourceNotFound = true)
public class DebiasService extends SerialisationUtils {

    protected static final Logger LOG = LogManager.getLogger(DebiasService.class);

    @Value("${debias.endpoint:}")
    private String debiasEndpoint;

    private final ObjectMapper mapper = new ObjectMapper();

    private HttpClient httpClient;

    public DebiasService() {
    }

    /**
     * Testing purposes. If we want to create the instance of Debias service
     * @param debiasEndpoint
     */
    public DebiasService (String debiasEndpoint) {
        this.debiasEndpoint = debiasEndpoint;
        init();
    }

    /**
     * Creates a new Http2 client that can send requests to debias client. Note that the client needs
     * to be closed when it's not used anymore
     */
    @PostConstruct
    private void init() {
        httpClient = HttpClient
                .newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();

        LOG.info("Http2 client initialised for the debias endpoint {} ", debiasEndpoint);

        SimpleModule module = new SimpleModule();
        module.addSerializer(Context.class, ContextSerializer.INSTANCE);
        mapper.registerModule(module);
        mapper.findAndRegisterModules();
        LOG.info("Object mapper initialized ... ");

    }

    /**
     * Fetch the List of Annotations from the Debias client for the request
     *
     * @param request request to be sent
     * @return list of annotations
     * @throws DebiasException
     */
    public List<Annotation> getAnnotationsForBiasTerms(DebiasRequest request) throws DebiasException {
        HttpRequest post = createRequest(debiasEndpoint, request);
        return   sendRequestAndGetResponse(post);
    }

    private HttpRequest createRequest(String debiasEndpoint, DebiasRequest request) throws DebiasException {
        try (OutputStream stream = new ByteArrayOutputStream()) {
            serialise(mapper, request, stream);
            return HttpRequest
                    .newBuilder(URI.create(debiasEndpoint))
                    .POST(HttpRequest.BodyPublishers.ofString(stream.toString()))
                    .setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate")
                    .build();
        } catch (IOException e) {
            throw new DebiasException(e.getMessage());
        }
    }

    // TODO error messages from Debias are in a very complex structure. Would be nice to have some solution for that. To know what excatly went wrong
    // For now whole error response body is sent if there is an error in the exception
    private List<Annotation> sendRequestAndGetResponse(HttpRequest post) throws DebiasException {
        try {
            HttpResponse<String> response = httpClient.send(post, HttpResponse.BodyHandlers.ofString());
            int httpStatusCode = response.statusCode();
            if (httpStatusCode != HttpStatus.SC_OK) {
                throw new IOException("Error from Debias API: " +
                        httpStatusCode + " - " + response.body());
            } else {
                return deserialize(mapper, response.body());
            }
        } catch (IOException | InterruptedException e) {
            throw new DebiasException(e.getMessage(), e);
        } catch (JsonParseException e) {
            throw new DebiasException("Error from AnnotationLdParser while deserializing response  - " + e.getMessage(), e);
        }
    }
}

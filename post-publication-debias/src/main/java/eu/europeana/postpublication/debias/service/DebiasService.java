package eu.europeana.postpublication.debias.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import eu.europeana.annotation.definitions.exception.AnnotationValidationException;
import eu.europeana.annotation.definitions.model.Annotation;
import eu.europeana.postpublication.debias.exception.DebiasException;
import eu.europeana.postpublication.debias.io.ContextSerializer;
import eu.europeana.postpublication.debias.io.CustomHttpResponseHandler;
import eu.europeana.postpublication.debias.model.Context;
import eu.europeana.postpublication.debias.model.DebiasRequest;
import eu.europeana.postpublication.debias.utils.SerialisationUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import static eu.europeana.postpublication.debias.utils.AppConstants.MAX_CONNECTIONS;
import static eu.europeana.postpublication.debias.utils.AppConstants.MAX_CONNECTIONS_PER_ROUTE;

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

    private CloseableHttpClient debiasClient;

    private final ObjectMapper mapper = new ObjectMapper();

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
     * Creates a new client that can send requests to debias client. Note that the client needs
     * to be closed when it's not used anymore
     */
    @PostConstruct
    private void init() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(MAX_CONNECTIONS);
        cm.setDefaultMaxPerRoute(MAX_CONNECTIONS_PER_ROUTE);
        cm.setDefaultSocketConfig(SocketConfig.custom().setSoKeepAlive(true).setSoTimeout(Timeout.ofMilliseconds(3600000)).build());
        debiasClient = HttpClients.custom().setConnectionManager(cm).build();
        LOG.info("Debias service is initialized with Endpoint - {}", debiasEndpoint);

        SimpleModule module = new SimpleModule();
        module.addSerializer(Context.class, ContextSerializer.INSTANCE);
        mapper.registerModule(module);
        mapper.findAndRegisterModules();
        LOG.info("Object mapper initialized ... ");

    }

    /**
     * Fetch the List of Annotations from the Debias client for the request
     * @param request request to be sent
     * @return list of annotations
     * @throws DebiasException
     */
    public List<Annotation> getAnnotationsForBiasTerms(DebiasRequest request) throws DebiasException {
        HttpPost post = createRequest(debiasEndpoint, request);
        List<Annotation> response = sendRequestAndGetResponse(post);
        return response;
    }

    private HttpPost createRequest(String debiasEndpoint, DebiasRequest request) throws DebiasException {
        try (OutputStream stream = new ByteArrayOutputStream()) {
            HttpPost post = new HttpPost(debiasEndpoint);
            serialise(mapper, request, stream);
            post.setEntity(new StringEntity(stream.toString()));

            post.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
            post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Sending POST {}", debiasEndpoint);
                LOG.trace("  body {}", request);
                LOG.trace("  headers:");
                for (Header header : post.getHeaders()) {
                    LOG.trace("  {}: {}", header.getName(), header.getValue());
                }
            }
            return post;
        } catch (IOException e) {
            throw new DebiasException(e.getMessage());
        }
    }

    private List<Annotation> sendRequestAndGetResponse(HttpPost post) throws DebiasException {
        try {
            HttpClientResponseHandler<List<Annotation>> responseHandler = new CustomHttpResponseHandler(mapper);
            List<Annotation> response = debiasClient.execute(post, responseHandler);
            if (response == null) {
                throw new DebiasException("Empty response from client");
            }
            return response;
        } catch (IOException e) {
            throw new DebiasException(e.getMessage());
        } catch (AnnotationValidationException e) {
            throw new DebiasException(e.getMessage(), e);
        }
    }
}

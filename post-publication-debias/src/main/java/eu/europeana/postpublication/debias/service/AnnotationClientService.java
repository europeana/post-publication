package eu.europeana.postpublication.debias.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.annotation.client.WebAnnotationProtocolApi;
import eu.europeana.annotation.definitions.model.Annotation;
import eu.europeana.annotation.definitions.model.vocabulary.MotivationTypes;
import eu.europeana.postpublication.debias.exception.DebiasException;
import eu.europeana.postpublication.debias.utils.SerialisationUtils;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import static eu.europeana.postpublication.debias.utils.AppConstants.POST_PUBLICATION_USER;

@Service
@PropertySource("classpath:post-publication.properties")
@PropertySource(value = "classpath:post-publication.user.properties", ignoreResourceNotFound = true)
public class AnnotationClientService extends SerialisationUtils {

    private static final Logger LOG = LogManager.getLogger(AnnotationClientService.class);

    private final WebAnnotationProtocolApi webAnnotationProtocolApi;

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${annotation.id.baseUrl}")
    private String annotationIDEndPoint;

    @Autowired
    public AnnotationClientService(WebAnnotationProtocolApi webAnnotationProtocolApi) {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.webAnnotationProtocolApi = webAnnotationProtocolApi;
     }

    /**
     * Will send Web requests to annotation api via client to create annotations for motivation 'HIGHLIGHTING'
     * POST_PUBLICATION_USER user value is sent to annotation api. As we need a dedicated client for the pipeline
     * See - properties file for authentication user details
     *
     * @param annotations
     * @throws DebiasException
     */
    public void createAnnotations(List<? extends Annotation> annotations) throws DebiasException {
        for (Annotation annotation : annotations) {
            LOG.debug("Writing annotation {}", annotation.getBody().getValue());
            String req = createRequestForAnnotation(annotation);
            try {
                ResponseEntity<String> res = callAnnotationAPI(req);
                LOG.debug("Annotation Created : {}", res.getBody().toString());
            } catch (IOException e) {
                if (req != null) {
                    LOG.error("Failed Request : {} ", req);
                }

            }
        }
    }

    private String createRequestForAnnotation(Annotation annotation) throws DebiasException {
        try (OutputStream annotationApiInput = new ByteArrayOutputStream()) {
           serialiseAnnotation(annotationIDEndPoint, annotation, annotationApiInput);
           return annotationApiInput.toString();
        } catch (IOException e) {
            LOG.error("Error occurred during annotation api request creation !! ",e);
            throw new DebiasException(e.getMessage());
        }
    }

    private ResponseEntity<String> callAnnotationAPI(String requestJson) throws IOException {
        LOG.debug("Sending annotation api request - {}",requestJson);
        String oaType = MotivationTypes.HIGHLIGHTING.getOaType();
        ResponseEntity<String> res= webAnnotationProtocolApi.createAnnotation(requestJson, null, POST_PUBLICATION_USER);
        if( HttpStatus.SC_OK != res.getStatusCodeValue()){
               LOG.error("Error Response :  {} - {}", res.getStatusCode(),res.getBody());
               throw new IOException("Annotation Call failed !!");
        }
        return res;
    }
}

package eu.europeana.postpublication.debias.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.annotation.client.WebAnnotationProtocolApi;
import eu.europeana.annotation.definitions.model.Annotation;
import eu.europeana.annotation.definitions.model.vocabulary.MotivationTypes;
import eu.europeana.postpublication.debias.exception.DebiasException;
import eu.europeana.postpublication.debias.utils.SerialisationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import static eu.europeana.postpublication.debias.utils.AppConstants.POST_PUBLICATION_USER;

@Service
public class AnnotationClientService extends SerialisationUtils {

    private static final Logger LOG = LogManager.getLogger(AnnotationClientService.class);

    private final WebAnnotationProtocolApi webAnnotationProtocolApi;
    private final ObjectMapper mapper = new ObjectMapper();

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
        for(Annotation annotation : annotations) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Writing annotation {}", annotation.getBody().getValue());
            }
            try (OutputStream stream = new ByteArrayOutputStream()) {
                serialiseAnnotation(mapper, annotation, stream);
                webAnnotationProtocolApi.createAnnotation(stream.toString(), MotivationTypes.HIGHLIGHTING.getOaType(), POST_PUBLICATION_USER);
            } catch (IOException e) {
                throw new DebiasException(e.getMessage());
            }

        }
    }
}

package eu.europeana.postpublication.debias.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.ContextAttributes;
import eu.europeana.annotation.definitions.model.Annotation;
import eu.europeana.annotation.definitions.model.vocabulary.MotivationTypes;
import eu.europeana.annotation.utils.parse.AnnotationLdParser;
import eu.europeana.annotation.utils.serialize.AnnotationLdSerializer;
import eu.europeana.postpublication.debias.model.Context;
import eu.europeana.postpublication.debias.model.DebiasRequest;
import org.apache.stanbol.commons.exception.JsonParseException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import static eu.europeana.postpublication.debias.utils.AppConstants.ITEMS;
import static eu.europeana.postpublication.debias.utils.AppConstants.CONTEXT;

public class SerialisationUtils {

    private final AnnotationLdParser annotationLdParser = new AnnotationLdParser();

    /**
     * Serialise the debias request
     * @param request
     * @param stream
     * @throws IOException
     */
    protected void serialise(String annotationItemDataEndpoint,ObjectMapper mapper, DebiasRequest request, OutputStream stream) throws IOException {
        ContextAttributes attrs = ContextAttributes.getEmpty()
                .withSharedAttribute(CONTEXT, new Context(annotationItemDataEndpoint+"/"));
        mapper.setDefaultAttributes(attrs);
        mapper.writerWithDefaultPrettyPrinter().writeValues(stream).write(request);
    }

    /**
     * Deserializes the debias client response
     * @param json
     * @return
     * @throws JsonProcessingException
     * @throws JsonParseException
     */
    public List<Annotation> deserialize(ObjectMapper mapper, String json) throws JsonProcessingException, JsonParseException {
        List<Annotation> response = new ArrayList<>();
        // items
        JsonNode itemsNode = mapper.readTree(json).get(ITEMS);
        if (itemsNode.isArray()) {
            for (JsonNode item : itemsNode) {
                Annotation annotation = annotationLdParser.parseAnnotation(MotivationTypes.HIGHLIGHTING, String.valueOf(item));
                response.add(annotation);
            }
        }
        return response;
    }

    /**
     * Serialise the Annotation
     * @param annotation
     * @param stream
     * @throws IOException
     */
    public void serialiseAnnotation(String annotationUri ,Annotation annotation, OutputStream stream) throws IOException {
        AnnotationLdSerializer annotationLd = new AnnotationLdSerializer(annotation,annotationUri);
        stream.write((annotationLd.toString()).getBytes());
    }

}

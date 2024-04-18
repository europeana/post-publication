package eu.europeana.postpublication.debias.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.ContextAttributes;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import eu.europeana.annotation.definitions.model.Annotation;
import eu.europeana.annotation.definitions.model.vocabulary.MotivationTypes;
import eu.europeana.annotation.utils.parse.AnnotationLdParser;
import eu.europeana.postpublication.debias.model.Context;
import eu.europeana.postpublication.debias.model.DebiasRequest;
import eu.europeana.postpublication.debias.model.DebiasResponse;
import eu.europeana.postpublication.debias.model.PartOf;
import org.apache.commons.lang3.StringUtils;
import org.apache.stanbol.commons.exception.JsonParseException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import static eu.europeana.postpublication.debias.utils.AppConstants.ITEMS;
import static eu.europeana.postpublication.debias.utils.AppConstants.context;

public class SerialisationUtils {

    private final AnnotationLdParser annotationLdParser = new AnnotationLdParser();

    /**
     * Serialise the debias request
     * @param request
     * @param stream
     * @throws IOException
     */
    public void serialise(ObjectMapper mapper, DebiasRequest request, OutputStream stream) throws IOException {
        ContextAttributes attrs = ContextAttributes.getEmpty()
                .withSharedAttribute(context, new Context("http://data.europeana.eu/item/"));
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
}

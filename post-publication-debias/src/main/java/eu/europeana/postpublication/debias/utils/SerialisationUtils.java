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

public abstract class SerialisationUtils {

    public static final int MAX_CONNECTIONS = 100;
    public static final int MAX_CONNECTIONS_PER_ROUTE = 100;

    public static final String context = "@context";
    public static final String base = "@base";
    public static final String PARTOF = "partOf";
    public static final String TOTAL = "total";
    public static final String MODIFIED = "modified";
    public static final String ITEMS = "items";
    public static final String TYPE = "type";

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
    // TODO - wee only need annotations rest can go to the bin
    public List<Annotation> deserialize(ObjectMapper mapper, String json) throws JsonProcessingException, JsonParseException {
        List<Annotation> response = new ArrayList<>();

//        // @context
//        JsonNode contextNode = mapper.readTree(json).get(context);
//        JsonNode baseNode = null;
//        if (contextNode.isArray()) {
//            // fetch base
//            for (JsonNode value : contextNode) {
//                if (value.getNodeType().equals(JsonNodeType.OBJECT)) {
//                    baseNode = value.get(base);
//                }
//            }
//        }
//        response.setContext(new Context(baseNode.asText()));
//
//        // partOf
//        JsonNode partOfNode = mapper.readTree(json).get(PARTOF);
//        if (partOfNode.has(TYPE) && StringUtils.equals(partOfNode.get(TYPE).asText(), "AnnotationCollection" )) {
//            PartOf partOf = new PartOf();
//            if (partOfNode.has(TOTAL)) {
//                partOf.setTotal(partOfNode.get(TOTAL).asLong());
//            }
//            if (partOfNode.has(MODIFIED)) {
//                partOf.setModified(partOfNode.get(MODIFIED).asText());
//            }
//            response.setPartOf(partOf);
//        }

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

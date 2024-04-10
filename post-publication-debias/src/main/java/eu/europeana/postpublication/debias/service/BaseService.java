package eu.europeana.postpublication.debias.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.ContextAttributes;
import eu.europeana.annotation.definitions.model.Annotation;
import eu.europeana.annotation.definitions.model.vocabulary.MotivationTypes;
import eu.europeana.annotation.utils.parse.AnnotationLdParser;
import eu.europeana.postpublication.debias.model.Context;
import eu.europeana.postpublication.debias.model.DebiasRequest;
import eu.europeana.postpublication.debias.model.DebiasResponse;
import eu.europeana.postpublication.debias.model.PartOf;
import org.apache.stanbol.commons.exception.JsonParseException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

public class BaseService {

    public static final int MAX_CONNECTIONS = 100;
    public static final int MAX_CONNECTIONS_PER_ROUTE = 100;

    public static final String context = "@context";
    public static final String base = "@base";
    public static final String PARTOF = "partOf";
    public static final String TOTAL = "total";
    public static final String MODIFIED = "modified";
    public static final String ITEMS = "items";

    protected ObjectMapper mapper;
    private AnnotationLdParser annotationLdParser = new AnnotationLdParser();

    /**
     * Serialise the debias request
     * @param request
     * @param stream
     * @throws IOException
     */
    public void serialise(DebiasRequest request, OutputStream stream) throws IOException {
        ContextAttributes attrs = ContextAttributes.getEmpty()
                .withSharedAttribute(context, new Context("http://data.europeana.eu/item/")); // TODO see what is the correct base value
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
    // TODO in response there are multiple value of context, check that
    public DebiasResponse deserialize(String json) throws JsonProcessingException, JsonParseException {
        DebiasResponse response = new DebiasResponse();

        // @context
        JsonNode contextValue = mapper.readTree(json).get(context);
        JsonNode baseValue = contextValue.get(base);
        response.setContext(new Context(baseValue.asText()));

        // partOf
        JsonNode partOf = mapper.readTree(json).get(PARTOF);
        response.setPartOf(new PartOf(partOf.get(TOTAL).asLong(), partOf.get(MODIFIED).asText()));

        // items
        Iterator<JsonNode> items = (Iterator<JsonNode>) mapper.readTree(json).get(ITEMS);
        while (items.hasNext()) {
            JsonNode object = items.next();
            Annotation annotation = annotationLdParser.parseAnnotation(MotivationTypes.HIGHLIGHTING, String.valueOf(object));
            response.addItem(annotation);
        }

        return response;

    }

}

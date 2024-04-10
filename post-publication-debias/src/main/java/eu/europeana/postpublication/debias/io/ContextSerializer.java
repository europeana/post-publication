package eu.europeana.postpublication.debias.io;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import eu.europeana.postpublication.debias.model.Context;

import java.io.IOException;

public class ContextSerializer extends JsonSerializer<Context> {

    public static final ContextSerializer INSTANCE = new ContextSerializer();

    @Override
    public void serialize(Context context, JsonGenerator jgen,
                          SerializerProvider serializers) throws IOException {
        jgen.writeStartArray();
        jgen.writeString(context.getURI());
        jgen.writeStartObject();
        jgen.writeStringField("@base", context.getBase());
        jgen.writeEndObject();
        jgen.writeEndArray();
    }
}
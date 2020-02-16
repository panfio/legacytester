package ru.panfio.legacytester.dependencies;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.TextNode;

import java.io.IOException;
import java.time.Instant;

public class IsoInstantDeserializer
        extends JsonDeserializer<Instant> {

    @Override
    public Instant deserialize(
            JsonParser jp, DeserializationContext dc) throws IOException {
        ObjectCodec codec = jp.getCodec();
        TextNode node = codec.readTree(jp);
        String dateString = node.textValue();
        return Instant.parse(dateString);
    }
}

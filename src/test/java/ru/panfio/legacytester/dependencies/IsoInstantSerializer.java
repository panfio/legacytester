package ru.panfio.legacytester.dependencies;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.Instant;

public class IsoInstantSerializer
        extends JsonSerializer<Instant> {

    @Override
    public void serialize(Instant dateTime,
                          JsonGenerator jg,
                          SerializerProvider sp) throws IOException {
        jg.writeString(dateTime.toString());
    }

}

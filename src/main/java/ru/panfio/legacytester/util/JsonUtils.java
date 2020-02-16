package ru.panfio.legacytester.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public final class JsonUtils {
    private JsonUtils() {
        throw new RuntimeException("Utility class");
    }

    public static String toJson(Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return "";
        }
    }

    public static <T> T parse(String json, TypeReference<T> reference) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return (T) mapper.readValue(json, reference);
        } catch (IOException e) {
            throw new RuntimeException("Parse Error" + e.getMessage());
        }
    }
}

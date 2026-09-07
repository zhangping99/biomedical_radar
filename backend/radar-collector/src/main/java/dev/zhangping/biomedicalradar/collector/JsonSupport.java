package dev.zhangping.biomedicalradar.collector;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.Instant;

public final class JsonSupport {
    private JsonSupport() {
    }

    public static Gson gson() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .registerTypeAdapter(Instant.class, new JsonSerializer<Instant>() {
                    @Override
                    public JsonElement serialize(Instant source, Type type, JsonSerializationContext context) {
                        return source == null ? JsonNull.INSTANCE : new JsonPrimitive(source.toString());
                    }
                })
                .registerTypeAdapter(Instant.class, new JsonDeserializer<Instant>() {
                    @Override
                    public Instant deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
                        return json == null || json.isJsonNull() ? null : Instant.parse(json.getAsString());
                    }
                })
                .create();
    }
}

package io.kineticedge.ks101.consumer.serde;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;
import java.util.Map;

import static io.kineticedge.ks101.common.util.JsonUtil.objectMapper;

@Slf4j
public class JsonDeserializer<T> implements Deserializer<T> {



    @SuppressWarnings("unused")
    public JsonDeserializer() {
    }

    @Override
    public void configure(Map<String, ?> props, boolean isKey) {
    }

    @Override
    public T deserialize(String topic, byte[] bytes) {

        if (bytes == null)
            return null;

        try {
            JsonNode node = objectMapper().readTree(bytes);

            if (node.get("$type") == null || !node.get("$type").isTextual()) {
                throw new SerializationException("missing '$type' field.");
            }

            return read(node.get("$type").asText(), node);
        } catch (final IOException e) {
            throw new SerializationException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private T read(final String className, JsonNode jsonNode) {
        try {
            return (T) objectMapper().convertValue(jsonNode, Class.forName(className));
        } catch (final ClassNotFoundException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void close() {
    }
}
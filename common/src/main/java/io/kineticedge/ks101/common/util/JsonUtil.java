package io.kineticedge.ks101.common.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.kineticedge.ks101.common.InstantDeserializer;
import io.kineticedge.ks101.common.InstantSerializer;

import java.time.Instant;
import java.util.TimeZone;

public final class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                    .setTimeZone(TimeZone.getDefault())
                    .registerModule(new JavaTimeModule())
                    .registerModule(new SimpleModule("instant-module", new Version(1, 0, 0, null, "", ""))
                            .addSerializer(Instant.class, new InstantSerializer())
                            .addDeserializer(Instant.class, new InstantDeserializer())
                    )
            ;


    public static ObjectMapper objectMapper() {
        return OBJECT_MAPPER;
    }

}

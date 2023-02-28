package io.kineticedge.ks101.common.streams;

import io.kineticedge.ks101.common.util.KafkaEnvUtil;
import io.kineticedge.ks101.common.util.PropertiesUtil;
import io.kineticedge.ks101.consumer.serde.JsonSerde;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;

import java.util.HashMap;
import java.util.Map;

public class KafkaStreamsConfigUtil {

    public static Map<String, Object> properties(final String bootstrapServer, final String applicationId) {

        final Map<String, Object> defaults = Map.ofEntries(
                Map.entry(ProducerConfig.LINGER_MS_CONFIG, 100),
                Map.entry(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer),
                Map.entry(StreamsConfig.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT"),
                Map.entry(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName()),
                Map.entry(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, JsonSerde.class.getName()),
                Map.entry(StreamsConfig.APPLICATION_ID_CONFIG, applicationId),
                Map.entry(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
                Map.entry(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 100),
                Map.entry(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndContinueExceptionHandler.class),
                Map.entry(StreamsConfig.TOPOLOGY_OPTIMIZATION_CONFIG, StreamsConfig.OPTIMIZE),
                Map.entry(StreamsConfig.METRICS_RECORDING_LEVEL_CONFIG, "DEBUG"),
                Map.entry(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 2)
        );

        final Map<String, Object> map = new HashMap<>(defaults);

        // Kafka Stream settings in the property files take priority.
        //
        // This needs to be relative path to allow for local-development to use it as well, in docker container
        // this needs to be added to the /app directory as that is the working dir.
        //
        map.putAll(PropertiesUtil.load("./app.properties"));

        // environment wins
        map.putAll(KafkaEnvUtil.to("STREAMS_"));

        return map;
    }
}

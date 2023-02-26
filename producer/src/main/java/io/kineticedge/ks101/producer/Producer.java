package io.kineticedge.ks101.producer;

import io.kineticedge.ks101.event.CustomerEvent;
import io.kineticedge.ks101.consumer.serde.JsonSerializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;

@Slf4j
public class Producer {

    final KafkaProducer<String, CustomerEvent> kafkaProducer;

    private final Options options;

    public Producer(final Options options) {
        this.options = options;
        kafkaProducer = new KafkaProducer<>(properties(options));
    }

    public void close() {
        kafkaProducer.close();
    }

    public Future<RecordMetadata> publish(final String topic, final CustomerEvent customer) {
        log.info("Sending key={}, value={}", customer.getCustomerId(), customer);
        return kafkaProducer.send(new ProducerRecord<>(topic, null, null, customer.getCustomerId(), customer), (metadata, exception) -> {
            if (exception != null) {
                log.error("error producing to kafka", exception);
            } else {
                log.debug("topic={}, partition={}, offset={}", metadata.topic(), metadata.partition(), metadata.offset());
            }
        });
    }

    private Map<String, Object> properties(final Options options) {
        Map<String, Object> defaults = Map.ofEntries(
                Map.entry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, options.getBootstrapServers()),
                Map.entry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT"),
                Map.entry(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName()),
                Map.entry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName()),
                Map.entry(ProducerConfig.LINGER_MS_CONFIG, 50L),
                Map.entry(ProducerConfig.ACKS_CONFIG, "all")
        );

        Map<String, Object> map = new HashMap<>(defaults);

        return map;
    }

    private static void dumpRecord(final ConsumerRecord<String, String> record) {
        log.info("Record:\n\ttopic     : {}\n\tpartition : {}\n\toffset    : {}\n\tkey       : {}\n\tvalue     : {}", record.topic(), record.partition(), record.offset(), record.key(), record.value());
    }

    public static Properties toProperties(final Map<String, Object> map) {
        final Properties properties = new Properties();
        properties.putAll(map);
        return properties;
    }
}

package io.kineticedge.ks101.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.kineticedge.ks101.common.util.PropertiesUtil;
import io.kineticedge.ks101.consumer.serde.JsonDeserializer;
import io.kineticedge.ks101.domain.Customer360;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.kineticedge.ks101.common.util.JsonUtil.objectMapper;

@Slf4j
public class Customer360Consumer {

    private final Options options;

    private KafkaConsumer<String, Customer360> kafkaConsumer;

    private boolean run = true;

    private CountDownLatch latch = new CountDownLatch(1);

    public Customer360Consumer(final Options options) {
        this.options = options;
        this.kafkaConsumer = new KafkaConsumer<String, Customer360>(properties(options));
    }

    public void close() {
        run = false;

        try {
            latch.await(options.getPollDuration().toMillis() * 3, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException e) {
        }
    }

    public void consume() {

        kafkaConsumer.subscribe(Collections.singleton(options.getCustomer360Topic()), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> collection) {
                kafkaConsumer.commitSync();
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> collection) {
            }
        });

        while (run) {
            final ConsumerRecords<String, Customer360> records = kafkaConsumer.poll(options.getPollDuration());

            records.forEach(record -> {
                try {
                    log.info(objectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(record.value()));
                } catch (final JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        kafkaConsumer.close();

        latch.countDown();
    }

    private Map<String, Object> properties(final Options options) {
        Map<String, Object> defaults = Map.ofEntries(
                Map.entry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, options.getBootstrapServers()),
                Map.entry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT"),
                Map.entry(CommonClientConfigs.GROUP_ID_CONFIG, "GROUP_AAA"),
                Map.entry(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()),
                Map.entry(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class.getName())
        );

        Map<String, Object> map = new HashMap<>(defaults);

        map.putAll(PropertiesUtil.load("/mnt/secrets/connection.properties"));

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

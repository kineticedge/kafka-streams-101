package io.kineticedge.ks101.streams;

import io.kineticedge.ks101.common.streams.KafkaStreamsConfigUtil;
import io.kineticedge.ks101.common.streams.ShutdownHook;
import io.kineticedge.ks101.common.util.KafkaEnvUtil;
import io.kineticedge.ks101.common.util.PropertiesUtil;
import io.kineticedge.ks101.domain.*;
import io.kineticedge.ks101.event.*;
import io.kineticedge.ks101.consumer.serde.JsonSerde;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.processor.api.FixedKeyProcessor;
import org.apache.kafka.streams.processor.api.FixedKeyProcessorContext;
import org.apache.kafka.streams.processor.api.FixedKeyRecord;
import org.apache.kafka.streams.state.KeyValueStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Slf4j
public class Streams {


    private Map<String, Object> properties(final Options options) {
        return KafkaStreamsConfigUtil.properties(options.getBootstrapServers(), options.getApplicationId());
    }

    public void start(final Options options) {

        Properties p = toProperties(properties(options));

        log.info("starting streams : " + options.getClientId());

        final Topology topology = streamsBuilder(options).build(p);

        log.info("Topology:\n" + topology.describe());

        final KafkaStreams streams = new KafkaStreams(topology, p);

        streams.setUncaughtExceptionHandler(e -> {
            log.error("unhandled streams exception, shutting down (a warning of 'Detected that shutdown was requested. All clients in this app will now begin to shutdown' will repeat every 100ms for the duration of session timeout).", e);
            return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION;
        });

        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook(streams)));

    }

    private StreamsBuilder streamsBuilder(final Options options) {

        final var builder = new StreamsBuilder();

        final var materialized = Materialized.<String, Customer360, KeyValueStore<Bytes, byte[]>>as("customer360");

        final KStream<String, CustomerEvent> name = builder.<String, CustomerEvent>stream(options.getNamesTopics(), Consumed.as("name-input"));
        final KStream<String, CustomerEvent> email = builder.<String, CustomerEvent>stream(options.getEmailTopic(), Consumed.as("email-input"));
        final KStream<String, CustomerEvent> phone = builder.<String, CustomerEvent>stream(options.getPhoneTopic(), Consumed.as("phone-input"));

        name
                .merge(email, Named.as("merge-email"))
                .merge(phone, Named.as("merge-phone"))
                .peek((k, v) -> log.debug("key={}, value={}", k, v), Named.as("peek-in"))
                .processValues(() -> new FixedKeyProcessor<String, CustomerEvent, CustomerEvent>() {

                            private FixedKeyProcessorContext<String, CustomerEvent> context;

                            @Override
                            public void init(FixedKeyProcessorContext<String, CustomerEvent> context) {
                                this.context = context;
                            }

                            @Override
                            public void process(FixedKeyRecord<String, CustomerEvent> record) {
                                record.value().setTimestamp(Instant.ofEpochMilli(record.timestamp()));
                                context.forward(record);
                            }
                        },
                        Named.as("processValues")
                )
                .groupByKey(Grouped.as("groupByKey"))
                .aggregate(
                        Customer360::new,
                        (key, event, customer360) -> {

                            if (customer360.getCustomerId() == null) {
                                customer360.setCustomerId(event.getCustomerId());
                            }

                            if (event instanceof NameUpdated) {
                                update(customer360, (NameUpdated) event);
                            } else if (event instanceof EmailUpdated) {
                                update(customer360, (EmailUpdated) event);
                            } else if (event instanceof PhoneUpdated) {
                                update(customer360, (PhoneUpdated) event);
                            }

                            return customer360;
                        },
                        Named.as("aggregator"),
                        materialized
                )
                .toStream(Named.as("toStream"))
                .peek((k, v) -> log.debug("key={}, value={}", k, v), Named.as("peek-out"))
                .to(options.getCustomer360Topic(), Produced.as("customer360-output"));

        return builder;
    }

    private void update(final Customer360 customer360, NameUpdated update) {
        add(customer360.getNames(), new Historical<>(update.getName(), update.getTimestamp()));
        customer360.setName(update.getName());
    }

    private void update(final Customer360 customer360, EmailUpdated update) {
        add(customer360.getEmails(), new Historical<>(update.getEmail(), update.getTimestamp()));
    }

    private void update(final Customer360 customer360, PhoneUpdated update) {
        add(customer360.getPhones(), new Historical<>(update.getPhone(), update.getTimestamp()));
    }

    private <T> void add(List<Historical<T>> list, Historical<T> element) {
        if (list.size() == 0) {
            list.add(element);
        } else {
            Historical<T> prev = list.get(list.size() - 1);
            prev.setEnd(element.getStart());
            list.add(element);
        }
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

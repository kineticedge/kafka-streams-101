package io.kineticedge.ks101.streams;

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
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.*;
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
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class Streams {


    private static final Duration SHUTDOWN = Duration.ofSeconds(30);

    private Map<String, Object> properties(final Options options) {

        final Map<String, Object> defaults = Map.ofEntries(
                Map.entry(ProducerConfig.LINGER_MS_CONFIG, 100),
                // Map.entry(StreamsConfig.NUM_STANDBY_REPLICAS_CONFIG, 2);
                Map.entry(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, options.getBootstrapServers()),
                Map.entry(StreamsConfig.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT"),
                Map.entry(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName()),
                Map.entry(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, JsonSerde.class.getName()),
                Map.entry(StreamsConfig.APPLICATION_ID_CONFIG, options.getApplicationId()),
                Map.entry(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, options.getAutoOffsetReset()),
                Map.entry(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 100),
                //Map.entry(CommonClientConfigs.SESSION_TIMEOUT_MS_CONFIG, 10_000),
                Map.entry(StreamsConfig.CLIENT_ID_CONFIG, options.getClientId()),
                Map.entry(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndContinueExceptionHandler.class),
                Map.entry(StreamsConfig.TOPOLOGY_OPTIMIZATION_CONFIG, StreamsConfig.OPTIMIZE),
                Map.entry(StreamsConfig.METRICS_RECORDING_LEVEL_CONFIG, "DEBUG"),
                Map.entry(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 2)
        );


        final Map<String, Object> map = new HashMap<>(defaults);

        //
        // If set the consumer is treated as a static member; this ID must be unique for every member in the group.
        //
        // * if you look at docker/entrypoint.sh it uses the numerical value within docker to ensure a uniqe instance.
        //
        // * if you are running stand-alone, you need to set this uniquely for every instance you plan on running.
        //
        if (options.getGroupInstanceId() != null) {
            map.put(CommonClientConfigs.GROUP_INSTANCE_ID_CONFIG, options.getGroupInstanceId());
        }

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

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Runtime shutdown hook, state={}", streams.state());
            if (streams.state().isRunningOrRebalancing()) {

                // New to Kafka Streams 3.3, you can have the application leave the group on shutting down (when member.id / static membership is used).
                //
                // There are reasons to do this and not to do it; from a development standpoint this makes starting/stopping
                // the application a lot easier reducing the time needed to rejoin the group.
                boolean leaveGroup = true;

                log.info("closing KafkaStreams with leaveGroup={}", leaveGroup);

                KafkaStreams.CloseOptions closeOptions = new KafkaStreams.CloseOptions().timeout(SHUTDOWN).leaveGroup(leaveGroup);

                boolean isClean = streams.close(closeOptions);
                if (!isClean) {
                    System.out.println("KafkaStreams was not closed cleanly");
                }

            } else if (streams.state().isShuttingDown()) {
                log.info("Kafka Streams is already shutting down with state={}, will wait {} to ensure proper shutdown.", streams.state(), SHUTDOWN);
                boolean isClean = streams.close(SHUTDOWN);
                if (!isClean) {
                    System.out.println("KafkaStreams was not closed cleanly");
                }
                System.out.println("final KafkaStreams state=" + streams.state());
            }
        }));

    }

    private StreamsBuilder streamsBuilder(final Options options) {

        final var builder = new StreamsBuilder();

        final Serdes.ListSerde<String> listSerde = new Serdes.ListSerde<>(ArrayList.class, Serdes.String());

        final var materialized = Materialized.<String, List<String>, KeyValueStore<Bytes, byte[]>>as("phones")
                .withValueSerde(listSerde);

        final KStream<String, Customer360> name = builder.<String, Customer360>stream(options.getCustomer360Topic(), Consumed.as("customer360-input"));

        name
                .peek((k, v) -> log.debug("key={}, value={}", k, v), Named.as("peek-in"))
                .flatMap((key, value) -> {
                            final List<KeyValue<String, String>> results = new ArrayList<>();
                            value.getPhones().forEach(phoneHistorical -> {
                                if (phoneHistorical.getElement() != null) {
                                    String phone = phoneHistorical.getElement().getPhoneNumber().replaceAll("[^0-9]", "");
                                    results.add(new KeyValue<>(phone, value.getCustomerId()));
                                }
                            });
                            return results;
                        },
                        Named.as("extract-phone-numbers")
                )
                .groupByKey(Grouped.with("groupByPhoneNumber", null, Serdes.String()))
                .aggregate(
                        ArrayList::new,
                        (phoneNumber, customerId, agg) -> {
                            if (!agg.contains(customerId)) {
                                agg.add(customerId);
                            }
                            return agg;
                        },
                        Named.as("aggregator"),
                        materialized
                )
                .toStream(Named.as("toStream"))
                .peek((k, v) -> log.debug("key={}, value={}", k, v), Named.as("peek-out"))
                .filter((k, v) -> v.size() > 1)
                .peek((k, v) -> log.debug("key={}, value={}", k, v), Named.as("peek-out-filtered"))
                .to(options.getDuplicatePhonesTopic(), Produced.<String, List<String>>with(null, listSerde));

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

package io.kineticedge.ks101.streams;

import io.kineticedge.ks101.common.streams.KafkaStreamsConfigUtil;
import io.kineticedge.ks101.common.streams.ShutdownHook;
import io.kineticedge.ks101.domain.Customer360;
import io.kineticedge.ks101.domain.Historical;
import io.kineticedge.ks101.event.EmailUpdated;
import io.kineticedge.ks101.event.NameUpdated;
import io.kineticedge.ks101.event.PhoneUpdated;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Slf4j
public class Streams {

    private Map<String, Object> properties(final Options options) {
        return KafkaStreamsConfigUtil.properties(options.getBootstrapServers(), options.getApplicationId());
    }

    public void start(final Options options) {

        Properties p = toProperties(properties(options));

        log.info("starting streams");

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

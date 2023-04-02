package io.kineticedge.ks101.admin;


import io.kineticedge.ks101.common.util.PropertiesUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class Admin {

    final AdminClient kafkaAdmin;

    public Admin(final Options options) {
        this.kafkaAdmin = AdminClient.create(properties(options));
    }

    public Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> offsets(String topic, OffsetSpec spec) throws InterruptedException, ExecutionException {

        Map<String, TopicDescription> map = kafkaAdmin.describeTopics(Collections.singleton(topic)).allTopicNames().get();

        Map<TopicPartition, OffsetSpec> query = map.entrySet().stream()
                .flatMap(e -> e.getValue().partitions().stream().map(v -> Map.entry(e.getKey(), v.partition())))
                .collect(Collectors.toMap(e -> new TopicPartition(e.getKey(), e.getValue()), e -> spec));

        return kafkaAdmin.listOffsets(query).all().get();
    }

    /**
     * List topics and their non-default conifgurations.
     */
    public Map<String, Pair<Integer, Map<String, Object>>> topics() throws InterruptedException, ExecutionException {

        final Set<String> topics = kafkaAdmin.listTopics().names().get();

        final List<ConfigResource> resources = topics.stream()
                .map(s -> new ConfigResource(ConfigResource.Type.TOPIC, s))
                .collect(Collectors.toList());

        final Map<String, Pair<Integer, Map<String, Object>>> results = new HashMap<>();

        Map<String, TopicDescription> map = kafkaAdmin.describeTopics(topics).allTopicNames().get();
        map.forEach((topic, description) -> {
            results.put(topic, new ImmutablePair<>(description.partitions().size(), new HashMap<>()));
        });

        Map<ConfigResource, Config> configs = kafkaAdmin.describeConfigs(resources).all().get();


        configs.forEach((resource, config) -> {
            final Pair<Integer, Map<String, Object>> topicResult = results.get(resource.name());
            config.entries().forEach(c -> {
                if (!c.isDefault()) {
                    Map<String, Object> vv = topicResult.getRight();
                    vv.put(c.name(), c.value());
                }
            });
        });

        return results;
    }

    public void close() {
        kafkaAdmin.close();
    }

    private Map<String, Object> properties(final Options options) {
        Map<String, Object> defaults = Map.ofEntries(
                Map.entry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, options.getBootstrapServers()),
                Map.entry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "PLAINTEXT")
        );

        Map<String, Object> map = new HashMap<>(defaults);

        map.putAll(PropertiesUtil.load("/mnt/secrets/connection.properties"));

        return map;
    }

    public static Properties toProperties(final Map<String, Object> map) {
        final Properties properties = new Properties();
        properties.putAll(map);
        return properties;
    }
}

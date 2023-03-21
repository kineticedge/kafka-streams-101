package io.kineticedge.ks101.admin;


import io.kineticedge.ks101.common.util.PropertiesUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.config.ConfigResource;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
public class Admin {

    final AdminClient kafkaAdmin;

    private final Options options;

    public Admin(final Options options) {
        this.options = options;
        this.kafkaAdmin = AdminClient.create(properties(options));
    }

    public Map<String, Map<String, Object>> topics() throws InterruptedException, ExecutionException {

        final Set<String> topics = kafkaAdmin.listTopics().names().get();

        final List<ConfigResource> resources = topics.stream()
                .map(s -> new ConfigResource(ConfigResource.Type.TOPIC, s))
                .collect(Collectors.toList());

        final Map<String, Map<String, Object>> results = new HashMap<>();

        Map<String, TopicDescription> map = kafkaAdmin.describeTopics(topics).allTopicNames().get();
        map.forEach((topic, description) -> {
            results.put(topic, new HashMap<>());
        });

        Map<ConfigResource, Config> configs = kafkaAdmin.describeConfigs(resources).all().get();


        configs.forEach((resource, config) -> {
            final Map<String, Object> topicResult = results.get(resource.name());
            config.entries().forEach(c -> {
                if (!c.isDefault()) {
                    topicResult.put(c.name(), c.value());
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

package io.kineticedge.ks101.producer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.kineticedge.ks101.common.InstantDeserializer;
import io.kineticedge.ks101.common.InstantSerializer;
import io.kineticedge.ks101.event.*;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.io.IOException;
import java.time.Instant;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ServletDeployment {

    private static final int PORT = 8080;

    private ObjectMapper objectMapper = new ObjectMapper()
            .setTimeZone(TimeZone.getDefault())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .registerModule(new JavaTimeModule())
            .registerModule(new SimpleModule("instant-module", new Version(1, 0, 0, null, "", ""))
                    .addSerializer(Instant.class, new InstantSerializer())
                    .addDeserializer(Instant.class, new InstantDeserializer())
            )
            ;

    private final Producer producer;

    private Undertow server;

    private String namesTopic;
    private String emailTopic;
    private String phoneTopic;

    public ServletDeployment(final Options options) {
        producer = new Producer(options);

        namesTopic = options.getNamesTopics();
        emailTopic = options.getEmailTopic();
        phoneTopic = options.getPhoneTopic();
    }

    public void start() {
        DeploymentInfo servletBuilder = Servlets.deployment()
                .setClassLoader(ServletDeployment.class.getClassLoader())
                .setContextPath("/")
                .setDeploymentName("streams");

        DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        manager.deploy();

        RoutingHandler routingHandler = Handlers.routing();

        routingHandler
                .add("POST", "/name", new BlockingHandler(exchange -> {
                            final Future<RecordMetadata> future = producer.publish(namesTopic, create(exchange, NameUpdated.class));
                            createResponse(exchange, future);
                        })
                )
                .add("POST", "/email", new BlockingHandler(exchange -> {
                            final Future<RecordMetadata> future = producer.publish(emailTopic, create(exchange, EmailUpdated.class));
                            createResponse(exchange, future);
                        })
                )
                .add("POST", "/phone", new BlockingHandler(exchange -> {
                            final Future<RecordMetadata> future = producer.publish(phoneTopic, create(exchange, PhoneUpdated.class));
                            createResponse(exchange, future);
                        })
                )
        ;

        server = Undertow.builder()
                .addHttpListener(PORT, "0.0.0.0")
                .setHandler(routingHandler)
                .build();
        server.start();
    }

    private void createResponse(HttpServerExchange exchange, Future<RecordMetadata> future) throws InterruptedException, ExecutionException {
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
        exchange.setStatusCode(StatusCodes.ACCEPTED);

        ProducerMetadata metadata = new ProducerMetadata(future.get());

        exchange.getResponseSender().send(write(metadata));
    }

    public void stop() {
        if (server != null) {
            server.stop();
        }

        producer.close();
    }


    private <T> T create(final HttpServerExchange exchange, final Class<T> clazz) throws IOException {
        ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(exchange.getInputStream());
        jsonNode.put("$type", clazz.getName());

        System.out.println(objectMapper.writeValueAsString(jsonNode));
        return read(jsonNode, clazz);
    }

    private <T> T read(final JsonNode jsonNode, final Class<T> clazz) {
        return (T) objectMapper.convertValue(jsonNode, clazz);
    }

    private <T> String write(final T object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}

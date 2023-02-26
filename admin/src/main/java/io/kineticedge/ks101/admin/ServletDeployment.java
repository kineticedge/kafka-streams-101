package io.kineticedge.ks101.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

import java.util.Map;

import static io.kineticedge.ks101.common.util.JsonUtil.objectMapper;

public class ServletDeployment {

    private static final int PORT = 8080;

//    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
//            .setTimeZone(TimeZone.getDefault())
//            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
//            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
//            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
//            .registerModule(new JavaTimeModule());

    private Undertow server;

    private Admin admin;

    public ServletDeployment(final Options options) {

        admin = new Admin(options);

    }

    public void start() {
        DeploymentInfo servletBuilder = Servlets.deployment()
                .setClassLoader(ServletDeployment.class.getClassLoader())
                .setContextPath("/")
                .setDeploymentName("admin");

        DeploymentManager manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        manager.deploy();

        RoutingHandler routingHandler = Handlers.routing();

        routingHandler
                .add("GET", "/topics", new BlockingHandler(exchange -> {
                            final Map<String, Map<String, Object>> result = admin.topics();
                            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                            exchange.setStatusCode(StatusCodes.ACCEPTED);
                            exchange.getResponseSender().send(format(result));
                        })
                );

        server = Undertow.builder()
                .addHttpListener(PORT, "0.0.0.0")
                .setHandler(routingHandler)
                .build();
        server.start();
    }


    private String format(final Map<String, Map<String, Object>> result) {
        try {
            return objectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result) + "\n";
        } catch (final JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}

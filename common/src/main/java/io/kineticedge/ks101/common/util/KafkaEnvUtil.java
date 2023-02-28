package io.kineticedge.ks101.common.util;

import java.util.Map;
import java.util.stream.Collectors;


public class KafkaEnvUtil {

    private KafkaEnvUtil() {
    }

    /**
     * Takes all environment variables that start with the given preifx, and return them with the key modified
     * to exclude the prefix, be lower-case, and replace '_' with '.'.
     */
    public static Map<String, String> to(final String prefix) {
        return System.getenv().entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix))
                .map(e -> {
                    final String key = e.getKey().substring(prefix.length()).replaceAll("_", ".").toLowerCase();
                    return Map.entry(key, e.getValue());
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}

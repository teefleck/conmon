package com.example.conmon.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigurationFileEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "conmonJsonConfiguration";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String file = environment.getProperty("conmon.configuration.file");
        if (file == null || file.isBlank()) {
            return;
        }
        Path path = Path.of(file);
        if (!Files.isRegularFile(path)) {
            return;
        }
        try {
            JsonNode monitor = new ObjectMapper().readTree(path.toFile()).path("monitor");
            if (monitor.isMissingNode() || !monitor.isObject()) {
                return;
            }
            Map<String, Object> properties = new LinkedHashMap<>();
            putIfPresent(properties, monitor, "enabled", "conmon.monitor.enabled");
            putIfPresent(properties, monitor, "scanIntervalMillis", "conmon.monitor.scan-interval-millis");
            putIfPresent(properties, monitor, "eventRetention", "conmon.monitor.event-retention");
            putIfPresent(properties, monitor, "eventPersistenceEnabled", "conmon.monitor.event-persistence-enabled");
            putIfPresent(properties, monitor, "procNetTcpPath", "conmon.monitor.proc-net-tcp-path");
            putIfPresent(properties, monitor, "procNetTcp6Path", "conmon.monitor.proc-net-tcp6-path");
            if (!properties.isEmpty()) {
                environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read ConMon JSON configuration file: " + path, ex);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private static void putIfPresent(Map<String, Object> properties, JsonNode monitor, String jsonName, String propertyName) {
        JsonNode value = monitor.get(jsonName);
        if (value != null && !value.isNull()) {
            properties.put(propertyName, value.isValueNode() ? value.asText() : value.toString());
        }
    }
}

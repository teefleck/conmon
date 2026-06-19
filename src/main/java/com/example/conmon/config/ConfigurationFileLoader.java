package com.example.conmon.config;

import com.example.conmon.dto.ConfigurationDocument;
import com.example.conmon.service.ConfigurationDocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Order(0)
public class ConfigurationFileLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationFileLoader.class);

    private final ConfigurationFileProperties properties;
    private final ObjectMapper objectMapper;
    private final ConfigurationDocumentService configurationDocumentService;

    public ConfigurationFileLoader(ConfigurationFileProperties properties, ObjectMapper objectMapper,
                                   ConfigurationDocumentService configurationDocumentService) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.configurationDocumentService = configurationDocumentService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (properties.file().isBlank()) {
            return;
        }
        Path path = Path.of(properties.file());
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Configuration file does not exist: " + path);
        }
        ConfigurationDocument document = objectMapper.readValue(path.toFile(), ConfigurationDocument.class);
        configurationDocumentService.importConfiguration(document, properties.replaceExisting(), true);
        log.info("Loaded ConMon configuration from {}", path);
    }
}

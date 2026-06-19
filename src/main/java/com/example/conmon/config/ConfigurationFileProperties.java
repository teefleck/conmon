package com.example.conmon.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "conmon.configuration")
public record ConfigurationFileProperties(
        String file,
        boolean replaceExisting,
        boolean readOnly
) {
    public ConfigurationFileProperties {
        if (file == null) {
            file = "";
        }
    }
}

package com.example.conmon.dto;

import java.time.Instant;
import java.util.List;

public record ConfigurationDocument(
        Instant exportedAt,
        boolean readOnly,
        String sourceFile,
        MonitorConfigurationDocument monitor,
        List<ConfigurationServiceEntry> services,
        List<ConfigurationClientEntry> clients
) {
}

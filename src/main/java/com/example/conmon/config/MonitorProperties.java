package com.example.conmon.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "conmon.monitor")
public record MonitorProperties(
        boolean enabled,
        @Min(1) long scanIntervalMillis,
        @Min(1) int eventRetention,
        boolean eventPersistenceEnabled,
        @NotBlank String procNetTcpPath,
        @NotBlank String procNetTcp6Path
) {
    public MonitorProperties {
        if (scanIntervalMillis == 0) {
            scanIntervalMillis = Duration.ofSeconds(1).toMillis();
        }
        if (eventRetention == 0) {
            eventRetention = 10_000;
        }
        if (procNetTcpPath == null || procNetTcpPath.isBlank()) {
            procNetTcpPath = "/proc/net/tcp";
        }
        if (procNetTcp6Path == null || procNetTcp6Path.isBlank()) {
            procNetTcp6Path = "/proc/net/tcp6";
        }
    }
}

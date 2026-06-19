package com.example.conmon.dto;

public record MonitorConfigurationDocument(
        boolean enabled,
        long scanIntervalMillis,
        int eventRetention,
        boolean eventPersistenceEnabled,
        String procNetTcpPath,
        String procNetTcp6Path
) {
}

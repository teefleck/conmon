package com.example.conmon.dto;

import java.time.Instant;

public record MonitorStatusResponse(
        Instant lastScanTime,
        int activeConnections,
        int configuredServices,
        int configuredClients,
        long connectionEvents,
        long connectedEvents,
        long disconnectedEvents,
        long lastScanDurationMs,
        String lastError
) {
}

package com.example.conmon.dto;

import java.time.Instant;
import java.util.UUID;

public record ClientStatusResponse(
        UUID clientId,
        String clientIp,
        String clientName,
        String status,
        Instant lastConnected,
        Instant lastSeen,
        Instant lastDisconnected
) {
}

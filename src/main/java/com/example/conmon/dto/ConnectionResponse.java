package com.example.conmon.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record ConnectionResponse(
        UUID serviceId,
        String serviceName,
        UUID clientId,
        String clientName,
        String localIp,
        int localPort,
        String remoteIp,
        int remotePort,
        Set<String> tags,
        Instant connectedAt,
        Instant lastSeenAt
) {
}

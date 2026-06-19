package com.example.conmon.dto;

import com.example.conmon.monitor.ConnectionEventType;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record EventResponse(
        UUID id,
        ConnectionEventType type,
        UUID serviceId,
        String serviceName,
        UUID clientId,
        String clientName,
        String localIp,
        int localPort,
        String remoteIp,
        int remotePort,
        Set<String> tags,
        Instant occurredAt
) {
}

package com.example.conmon.monitor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record ConnectionEvent(
        UUID id,
        ConnectionEventType type,
        ConnectionKey key,
        String serviceName,
        String clientName,
        Set<String> tags,
        Instant occurredAt
) {
    public static ConnectionEvent of(ConnectionEventType type, ActiveConnection connection, Instant occurredAt) {
        return new ConnectionEvent(
                UUID.randomUUID(),
                type,
                connection.key(),
                connection.serviceName(),
                connection.clientName(),
                connection.tags(),
                occurredAt
        );
    }
}

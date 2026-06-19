package com.example.conmon.monitor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record ActiveConnection(
        ConnectionKey key,
        String serviceName,
        String clientName,
        Set<String> tags,
        Instant connectedAt,
        Instant lastSeenAt
) {
    public ActiveConnection withLastSeenAt(Instant lastSeenAt) {
        return new ActiveConnection(key, serviceName, clientName, tags, connectedAt, lastSeenAt);
    }

    public UUID serviceId() {
        return key.serviceId();
    }

    public UUID clientId() {
        return key.clientId();
    }
}

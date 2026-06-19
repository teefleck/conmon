package com.example.conmon.service;

import com.example.conmon.dto.ConnectionResponse;
import com.example.conmon.dto.EventResponse;
import com.example.conmon.monitor.ActiveConnection;
import com.example.conmon.monitor.ConnectionEvent;
import com.example.conmon.monitor.ConnectionRegistry;
import com.example.conmon.monitor.EventStore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class ConnectionQueryService {

    private final ConnectionRegistry registry;
    private final EventStore eventStore;

    public ConnectionQueryService(ConnectionRegistry registry, EventStore eventStore) {
        this.registry = registry;
        this.eventStore = eventStore;
    }

    public List<ConnectionResponse> activeConnections(Optional<UUID> serviceId, Optional<UUID> clientId, Set<String> tags) {
        return registry.all().stream()
                .filter(connection -> serviceId.map(id -> connection.serviceId().equals(id)).orElse(true))
                .filter(connection -> clientId.map(id -> connection.clientId().equals(id)).orElse(true))
                .filter(connection -> tags == null || tags.isEmpty() || connection.tags().containsAll(tags))
                .map(this::toResponse)
                .toList();
    }

    public List<EventResponse> events(Optional<UUID> serviceId, Optional<UUID> clientId,
                                      Optional<Instant> from, Optional<Instant> to) {
        return eventStore.query(serviceId, clientId, from, to).stream().map(this::toResponse).toList();
    }

    private ConnectionResponse toResponse(ActiveConnection connection) {
        var key = connection.key();
        return new ConnectionResponse(key.serviceId(), connection.serviceName(), key.clientId(), connection.clientName(),
                key.localIp(), key.localPort(), key.remoteIp(), key.remotePort(), connection.tags(),
                connection.connectedAt(), connection.lastSeenAt());
    }

    private EventResponse toResponse(ConnectionEvent event) {
        var key = event.key();
        return new EventResponse(event.id(), event.type(), key.serviceId(), event.serviceName(), key.clientId(),
                event.clientName(), key.localIp(), key.localPort(), key.remoteIp(), key.remotePort(), event.tags(),
                event.occurredAt());
    }
}

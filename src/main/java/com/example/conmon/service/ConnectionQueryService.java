package com.example.conmon.service;

import com.example.conmon.dto.ConnectionResponse;
import com.example.conmon.dto.ClientStatusResponse;
import com.example.conmon.dto.EventResponse;
import com.example.conmon.entity.MonitoredClient;
import com.example.conmon.monitor.ActiveConnection;
import com.example.conmon.monitor.ConnectionEvent;
import com.example.conmon.monitor.ConnectionEventType;
import com.example.conmon.monitor.ConnectionRegistry;
import com.example.conmon.monitor.EventStore;
import com.example.conmon.repository.MonitoredClientRepository;
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
    private final MonitoredClientRepository clientRepository;

    public ConnectionQueryService(ConnectionRegistry registry, EventStore eventStore,
                                  MonitoredClientRepository clientRepository) {
        this.registry = registry;
        this.eventStore = eventStore;
        this.clientRepository = clientRepository;
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

    public List<ClientStatusResponse> clientStatus(Optional<String> clientIp) {
        List<MonitoredClient> clients = clientIp.isEmpty() 
            ? clientRepository.findAll()
            : clientRepository.findAll().stream()
                .filter(c -> c.getIpAddress().equals(clientIp.get()))
                .toList();

        return clients.stream().map(client -> {
            List<ConnectionEvent> clientEvents = eventStore.query(Optional.empty(), Optional.of(client.getId()),
                    Optional.empty(), Optional.empty());
            
            boolean hasActiveConnection = registry.all().stream()
                    .anyMatch(conn -> conn.clientId().equals(client.getId()));

            String status;
            if (hasActiveConnection) {
                status = "CONNECTED";
            } else if (!clientEvents.isEmpty()) {
                ConnectionEvent lastEvent = clientEvents.get(clientEvents.size() - 1);
                status = lastEvent.type() == ConnectionEventType.CONNECTED ? "CONNECTED" : "DISCONNECTED";
            } else {
                status = "DISCONNECTED";
            }

            Instant lastConnected = clientEvents.stream()
                    .filter(e -> e.type() == ConnectionEventType.CONNECTED)
                    .map(ConnectionEvent::occurredAt)
                    .max(Instant::compareTo)
                    .orElse(null);

            Instant lastDisconnected = clientEvents.stream()
                    .filter(e -> e.type() == ConnectionEventType.DISCONNECTED)
                    .map(ConnectionEvent::occurredAt)
                    .max(Instant::compareTo)
                    .orElse(null);

            Instant lastSeen = registry.all().stream()
                    .filter(conn -> conn.clientId().equals(client.getId()))
                    .map(ActiveConnection::lastSeenAt)
                    .max(Instant::compareTo)
                    .orElse(lastConnected != null && (lastDisconnected == null || lastConnected.isAfter(lastDisconnected))
                            ? lastConnected
                            : lastDisconnected);

            return new ClientStatusResponse(client.getId(), client.getIpAddress(), client.getClientName(),
                    status, lastConnected, lastSeen, lastDisconnected);
        }).toList();
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

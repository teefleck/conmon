package com.example.conmon.monitor;

import com.example.conmon.config.MonitorProperties;
import com.example.conmon.entity.ConnectionEventEntity;
import com.example.conmon.repository.ConnectionEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class EventStore {

    private final ArrayDeque<ConnectionEvent> events = new ArrayDeque<>();
    private final int retention;
    private final boolean persistenceEnabled;
    private final ConnectionEventRepository repository;
    private final Counter allEventsCounter;
    private final Counter connectedCounter;
    private final Counter disconnectedCounter;
    private long totalEvents;
    private long connectedEvents;
    private long disconnectedEvents;

    public EventStore(MonitorProperties properties, ConnectionEventRepository repository, MeterRegistry meterRegistry) {
        this.retention = properties.eventRetention();
        this.persistenceEnabled = properties.eventPersistenceEnabled();
        this.repository = repository;
        this.allEventsCounter = Counter.builder("connection_events_total").register(meterRegistry);
        this.connectedCounter = Counter.builder("connected_events_total").register(meterRegistry);
        this.disconnectedCounter = Counter.builder("disconnected_events_total").register(meterRegistry);
    }

    @Transactional
    public void append(ConnectionEvent event) {
        synchronized (events) {
            events.addLast(event);
            while (events.size() > retention) {
                events.removeFirst();
            }
            totalEvents++;
            if (event.type() == ConnectionEventType.CONNECTED) {
                connectedEvents++;
            } else {
                disconnectedEvents++;
            }
        }
        allEventsCounter.increment();
        if (event.type() == ConnectionEventType.CONNECTED) {
            connectedCounter.increment();
        } else {
            disconnectedCounter.increment();
        }
        if (persistenceEnabled) {
            repository.save(toEntity(event));
        }
    }

    public List<ConnectionEvent> query(Optional<java.util.UUID> serviceId, Optional<java.util.UUID> clientId,
                                       Optional<Instant> from, Optional<Instant> to) {
        synchronized (events) {
            return events.stream()
                    .filter(event -> serviceId.map(id -> event.key().serviceId().equals(id)).orElse(true))
                    .filter(event -> clientId.map(id -> event.key().clientId().equals(id)).orElse(true))
                    .filter(event -> from.map(start -> !event.occurredAt().isBefore(start)).orElse(true))
                    .filter(event -> to.map(end -> !event.occurredAt().isAfter(end)).orElse(true))
                    .toList();
        }
    }

    public List<ConnectionEvent> all() {
        synchronized (events) {
            return new ArrayList<>(events);
        }
    }

    public long totalEvents() {
        synchronized (events) {
            return totalEvents;
        }
    }

    public long connectedEvents() {
        synchronized (events) {
            return connectedEvents;
        }
    }

    public long disconnectedEvents() {
        synchronized (events) {
            return disconnectedEvents;
        }
    }

    private ConnectionEventEntity toEntity(ConnectionEvent event) {
        ConnectionKey key = event.key();
        return new ConnectionEventEntity(
                event.id(),
                event.type(),
                key.serviceId(),
                event.serviceName(),
                key.clientId(),
                event.clientName(),
                key.localIp(),
                key.localPort(),
                key.remoteIp(),
                key.remotePort(),
                event.occurredAt(),
                event.tags()
        );
    }
}

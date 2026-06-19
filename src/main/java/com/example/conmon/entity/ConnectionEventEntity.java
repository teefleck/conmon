package com.example.conmon.entity;

import com.example.conmon.monitor.ConnectionEventType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "connection_events")
public class ConnectionEventEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConnectionEventType type;

    @Column(nullable = false)
    private UUID serviceId;
    @Column(nullable = false)
    private String serviceName;
    @Column(nullable = false)
    private UUID clientId;
    @Column(nullable = false)
    private String clientName;
    @Column(nullable = false)
    private String localIp;
    @Column(nullable = false)
    private int localPort;
    @Column(nullable = false)
    private String remoteIp;
    @Column(nullable = false)
    private int remotePort;
    @Column(nullable = false)
    private Instant occurredAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "connection_event_tags", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "tag", nullable = false)
    private Set<String> tags = new LinkedHashSet<>();

    protected ConnectionEventEntity() {
    }

    public ConnectionEventEntity(UUID id, ConnectionEventType type, UUID serviceId, String serviceName, UUID clientId,
                                 String clientName, String localIp, int localPort, String remoteIp, int remotePort,
                                 Instant occurredAt, Set<String> tags) {
        this.id = id;
        this.type = type;
        this.serviceId = serviceId;
        this.serviceName = serviceName;
        this.clientId = clientId;
        this.clientName = clientName;
        this.localIp = localIp;
        this.localPort = localPort;
        this.remoteIp = remoteIp;
        this.remotePort = remotePort;
        this.occurredAt = occurredAt;
        this.tags = tags == null ? new LinkedHashSet<>() : new LinkedHashSet<>(tags);
    }
}

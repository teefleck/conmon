package com.example.conmon.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "services")
public class MonitoredService {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String serviceName;

    @Column(length = 2048)
    private String description;

    @Column(nullable = false)
    private int port;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "service_tags", joinColumns = @JoinColumn(name = "service_id"))
    @Column(name = "tag", nullable = false)
    private Set<String> tags = new LinkedHashSet<>();

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected MonitoredService() {
    }

    public MonitoredService(String serviceName, String description, int port, Set<String> tags, boolean enabled) {
        this(UUID.randomUUID(), serviceName, description, port, tags, enabled);
    }

    public MonitoredService(UUID id, String serviceName, String description, int port, Set<String> tags, boolean enabled) {
        this.id = id == null ? UUID.randomUUID() : id;
        update(serviceName, description, port, tags, enabled);
    }

    public void update(String serviceName, String description, int port, Set<String> tags, boolean enabled) {
        this.serviceName = serviceName;
        this.description = description;
        this.port = port;
        this.tags = tags == null ? new LinkedHashSet<>() : new LinkedHashSet<>(tags);
        this.enabled = enabled;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getDescription() {
        return description;
    }

    public int getPort() {
        return port;
    }

    public Set<String> getTags() {
        return Set.copyOf(tags);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

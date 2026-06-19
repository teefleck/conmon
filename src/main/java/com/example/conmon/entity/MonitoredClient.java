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
@Table(name = "clients")
public class MonitoredClient {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String clientName;

    @Column(length = 2048)
    private String description;

    @Column(nullable = false)
    private String ipAddress;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "client_tags", joinColumns = @JoinColumn(name = "client_id"))
    @Column(name = "tag", nullable = false)
    private Set<String> tags = new LinkedHashSet<>();

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected MonitoredClient() {
    }

    public MonitoredClient(String clientName, String description, String ipAddress, Set<String> tags, boolean enabled) {
        this(UUID.randomUUID(), clientName, description, ipAddress, tags, enabled);
    }

    public MonitoredClient(UUID id, String clientName, String description, String ipAddress, Set<String> tags, boolean enabled) {
        this.id = id == null ? UUID.randomUUID() : id;
        update(clientName, description, ipAddress, tags, enabled);
    }

    public void update(String clientName, String description, String ipAddress, Set<String> tags, boolean enabled) {
        this.clientName = clientName;
        this.description = description;
        this.ipAddress = ipAddress;
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

    public String getClientName() {
        return clientName;
    }

    public String getDescription() {
        return description;
    }

    public String getIpAddress() {
        return ipAddress;
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

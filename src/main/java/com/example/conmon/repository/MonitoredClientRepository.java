package com.example.conmon.repository;

import com.example.conmon.entity.MonitoredClient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MonitoredClientRepository extends JpaRepository<MonitoredClient, UUID> {
    List<MonitoredClient> findByEnabledTrue();

    Optional<MonitoredClient> findByClientName(String clientName);
}

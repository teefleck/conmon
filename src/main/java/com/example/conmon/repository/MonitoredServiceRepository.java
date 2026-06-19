package com.example.conmon.repository;

import com.example.conmon.entity.MonitoredService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MonitoredServiceRepository extends JpaRepository<MonitoredService, UUID> {
    List<MonitoredService> findByEnabledTrue();

    Optional<MonitoredService> findByServiceName(String serviceName);
}

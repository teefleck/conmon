package com.example.conmon.repository;

import com.example.conmon.entity.ConnectionEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConnectionEventRepository extends JpaRepository<ConnectionEventEntity, UUID> {
}

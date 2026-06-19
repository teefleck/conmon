package com.example.conmon.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record ServiceResponse(
        UUID id,
        String serviceName,
        String description,
        int port,
        Set<String> tags,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}

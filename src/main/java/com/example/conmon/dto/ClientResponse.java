package com.example.conmon.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record ClientResponse(
        UUID id,
        String clientName,
        String description,
        String ipAddress,
        Set<String> tags,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}

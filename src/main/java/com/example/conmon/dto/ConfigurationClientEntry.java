package com.example.conmon.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;
import java.util.UUID;

public record ConfigurationClientEntry(
        UUID id,
        @NotBlank String clientName,
        String description,
        @NotBlank String ipAddress,
        Set<String> tags,
        boolean enabled
) {
}

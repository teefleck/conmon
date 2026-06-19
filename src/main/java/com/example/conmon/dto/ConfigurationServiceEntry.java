package com.example.conmon.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;
import java.util.UUID;

public record ConfigurationServiceEntry(
        UUID id,
        @NotBlank String serviceName,
        String description,
        @Min(1) @Max(65535) int port,
        Set<String> tags,
        boolean enabled
) {
}

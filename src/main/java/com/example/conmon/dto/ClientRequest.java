package com.example.conmon.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record ClientRequest(
        @NotBlank String clientName,
        String description,
        @NotBlank String ipAddress,
        Set<String> tags,
        boolean enabled
) {
}

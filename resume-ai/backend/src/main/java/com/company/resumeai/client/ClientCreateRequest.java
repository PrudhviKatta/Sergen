package com.company.resumeai.client;

import jakarta.validation.constraints.NotBlank;

public record ClientCreateRequest(
        @NotBlank String name,
        String industry,
        String description
) {
}

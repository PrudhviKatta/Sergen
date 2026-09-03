package com.company.resumeai.client;

import java.time.Instant;
import java.util.UUID;

public record ClientResponse(
        UUID id,
        String name,
        String normalizedName,
        String industry,
        String description,
        Instant createdAt
) {

    public static ClientResponse from(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getName(),
                client.getNormalizedName(),
                client.getIndustry(),
                client.getDescription(),
                client.getCreatedAt()
        );
    }
}

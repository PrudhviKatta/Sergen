package com.company.resumeai.knowledge;

import java.time.Instant;
import java.util.UUID;

public record KnowledgeFragmentResponse(
        UUID id,
        UUID candidateId,
        UUID clientId,
        UUID projectId,
        FragmentType fragmentType,
        String content,
        String domain,
        String role,
        Short startYear,
        Short endYear,
        boolean hasEmbedding,
        Instant createdAt
) {

    public static KnowledgeFragmentResponse from(KnowledgeFragment fragment) {
        return new KnowledgeFragmentResponse(
                fragment.getId(),
                fragment.getCandidateId(),
                fragment.getClientId(),
                fragment.getProjectId(),
                fragment.getFragmentType(),
                fragment.getContent(),
                fragment.getDomain(),
                fragment.getRole(),
                fragment.getStartYear(),
                fragment.getEndYear(),
                fragment.hasEmbedding(),
                fragment.getCreatedAt()
        );
    }
}

package com.company.resumeai.ingestion;

import com.fasterxml.jackson.annotation.JsonRawValue;

import java.time.Instant;
import java.util.UUID;

/**
 * @JsonRawValue embeds the already-serialized parsedJson string as real JSON
 * in the response rather than a quoted/escaped string - avoids deserializing
 * it back into ParsedResume just to re-serialize it, and null renders as
 * `null` normally (the annotation only changes non-null serialization).
 */
public record ResumeSourceResponse(
        UUID id,
        UUID candidateId,
        String fileName,
        String fileType,
        String rawText,
        @JsonRawValue String parsedJson,
        IngestionStatus status,
        String failureReason,
        Instant createdAt
) {

    public static ResumeSourceResponse from(ResumeSource source) {
        return new ResumeSourceResponse(
                source.getId(),
                source.getCandidateId(),
                source.getFileName(),
                source.getFileType(),
                source.getRawText(),
                source.getParsedJson(),
                source.getIngestionStatus(),
                source.getFailureReason(),
                source.getCreatedAt()
        );
    }
}

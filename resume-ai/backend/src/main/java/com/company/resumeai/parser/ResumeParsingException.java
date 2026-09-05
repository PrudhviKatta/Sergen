package com.company.resumeai.parser;

/**
 * Thrown when the LLM's resume-parsing response couldn't be parsed into
 * ParsedResume. Deliberately a RuntimeException, same reasoning as
 * llm.LlmGenerationException: a malformed provider response is an
 * operator/provider problem (or a prompt that needs tuning), not a bad
 * client request - ingestion.ResumeUploadService catches this alongside
 * LlmGenerationException/EmbeddingGenerationException and records it as a
 * FAILED resume_source row instead of a 500.
 */
public class ResumeParsingException extends RuntimeException {

    public ResumeParsingException(String message) {
        super(message);
    }

    public ResumeParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}

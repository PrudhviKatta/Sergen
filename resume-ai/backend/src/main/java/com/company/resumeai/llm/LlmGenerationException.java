package com.company.resumeai.llm;

/**
 * Thrown when the LLM couldn't produce usable output - no API key
 * configured, the provider call failed, or the response couldn't be parsed.
 * Deliberately a RuntimeException, same reasoning as
 * embedding.EmbeddingGenerationException: a missing OPENAI_API_KEY or a
 * malformed provider response is an operator/provider problem, not a bad
 * client request. ResumeGenerationService catches this at the top level and
 * records it as a FAILED generation rather than letting it become a 500 -
 * a single project's LLM call failing shouldn't look identical to an
 * unrelated bug in the request-handling code.
 */
public class LlmGenerationException extends RuntimeException {

    public LlmGenerationException(String message) {
        super(message);
    }

    public LlmGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}

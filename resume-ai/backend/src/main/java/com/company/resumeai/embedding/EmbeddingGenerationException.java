package com.company.resumeai.embedding;

/**
 * Thrown when an embedding couldn't be produced - no API key configured,
 * the provider call failed, or the response couldn't be parsed. Deliberately
 * a RuntimeException (not caught/translated by GlobalExceptionHandler as a
 * 400): a missing OPENAI_API_KEY is an operator/config problem, not a bad
 * client request, so it should surface as a 500 the same way any other
 * unexpected server error does.
 */
public class EmbeddingGenerationException extends RuntimeException {

    public EmbeddingGenerationException(String message) {
        super(message);
    }

    public EmbeddingGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}

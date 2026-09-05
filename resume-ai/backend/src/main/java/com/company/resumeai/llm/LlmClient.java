package com.company.resumeai.llm;

/**
 * §26 provider abstraction, chat-completion-specific — mirrors
 * embedding.EmbeddingClient's role for embeddings. OpenAiLlmClient is the
 * only implementation for now; a local/Ollama implementation (§27) can be
 * added later behind the same interface without touching ResumeGenerationService.
 */
public interface LlmClient {

    LlmResponse generate(LlmRequest request);
}

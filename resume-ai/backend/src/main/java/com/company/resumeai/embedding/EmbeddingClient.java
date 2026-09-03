package com.company.resumeai.embedding;

/**
 * §26 provider abstraction, embedding-specific: keeps the rest of the app
 * (KnowledgeFragmentService, RetrievalService) decoupled from which provider
 * actually computes embeddings. OpenAiEmbeddingClient is the only
 * implementation for now; a local/Ollama implementation can be added later
 * without touching any caller.
 */
public interface EmbeddingClient {

    /** @return the embedding vector for {@code text}, in this client's fixed dimension. */
    float[] embed(String text);
}

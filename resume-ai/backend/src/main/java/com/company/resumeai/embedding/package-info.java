/**
 * Embedding provider abstraction for knowledge fragments (§24, §26).
 *
 * Built (Milestone 3): {@link com.company.resumeai.embedding.EmbeddingClient}
 * (interface) with {@link com.company.resumeai.embedding.OpenAiEmbeddingClient}
 * (text-embedding-3-small, 1536 dims) as the only implementation so far, plus
 * {@link com.company.resumeai.embedding.VectorCodec} for converting between a
 * Java {@code float[]} and pgvector's text literal format. A local/Ollama
 * implementation can be added later behind the same interface.
 */
package com.company.resumeai.embedding;

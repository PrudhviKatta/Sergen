/**
 * §26 LLM chat-completion provider abstraction.
 *
 * Not one of §22's originally listed packages - it started as part of
 * {@code generation} (Milestone 5, the only consumer at the time) and moved
 * here once {@code parser} (Milestone 2, resume parsing) needed the same
 * {@link com.company.resumeai.llm.LlmClient} abstraction: once a second,
 * unrelated domain package needs it, it stops being generation-specific.
 * Same role as {@code embedding.EmbeddingClient} plays for embeddings, just
 * for chat completions - {@link com.company.resumeai.llm.OpenAiLlmClient} is
 * the only implementation so far.
 */
package com.company.resumeai.llm;

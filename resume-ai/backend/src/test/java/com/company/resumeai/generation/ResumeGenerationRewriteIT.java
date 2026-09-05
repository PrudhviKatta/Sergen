package com.company.resumeai.generation;

import com.company.resumeai.AbstractIntegrationTest;
import com.company.resumeai.embedding.EmbeddingClient;
import com.company.resumeai.llm.LlmClient;
import com.company.resumeai.llm.LlmResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.Random;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises §19's rewrite loop against a real Testcontainers Postgres: a
 * seeded knowledge fragment gives the fake LlmClient something to duplicate
 * on its first attempt (forcing a REWRITE verdict via
 * DuplicatePhraseDetector's exact-phrase match), then a different draft on
 * the retry (the fake distinguishes attempts by whether
 * ResumeGenerationService's REWRITE_HINT text is present in the user
 * prompt - no counters/mutable state needed, same "pure function of the
 * request" style as the other fake LLM/embedding beans in this test suite).
 */
@Import({ResumeGenerationRewriteIT.FakeEmbeddingConfig.class, ResumeGenerationRewriteIT.DuplicatingLlmConfig.class})
class ResumeGenerationRewriteIT extends AbstractIntegrationTest {

    private static final String DUPLICATE_CONTENT =
            "Modernized a legacy banking platform by implementing microservices architecture using Java and Spring Boot across many services";

    @TestConfiguration
    static class FakeEmbeddingConfig {
        @Bean
        @Primary
        EmbeddingClient fakeEmbeddingClient() {
            return text -> {
                Random random = new Random(text.hashCode());
                float[] vector = new float[1536];
                for (int i = 0; i < vector.length; i++) {
                    vector[i] = random.nextFloat() * 2f - 1f; // [-1,1) - symmetric, so unrelated texts average to ~0 cosine similarity
                }
                return vector;
            };
        }
    }

    @TestConfiguration
    static class DuplicatingLlmConfig {
        @Bean
        @Primary
        LlmClient duplicatingLlmClient() {
            return request -> {
                boolean isProjectPrompt = request.systemPrompt().contains("JSON object");
                if (!isProjectPrompt) {
                    return new LlmResponse("A candidate summary that mentions nothing verbatim.", "fake-model");
                }
                boolean isRewriteAttempt = request.userPrompt().contains("too similar to existing reference material");
                String description = isRewriteAttempt
                        ? "Delivered an entirely different narrative that shares no wording with any prior material here at all."
                        : DUPLICATE_CONTENT;
                return new LlmResponse("""
                        {"description": "%s", "responsibilities": ["Did one thing", "Did another thing"], "environment": ["Java 8"]}
                        """.formatted(description), "fake-model");
            };
        }
    }

    @Test
    void rewritesOnceWhenFirstDraftDuplicatesReferenceMaterial() throws Exception {
        String domain = "RewriteTest-" + UUID.randomUUID();
        seedKnowledgeFragment(domain);

        String requestBody = """
                {
                  "candidateName": "Candidate A",
                  "primaryRole": "Java Full Stack Developer",
                  "totalExperienceYears": 10,
                  "projects": [
                    { "client": "AT&T", "startDate": "2016-01-01", "endDate": "2019-06-30", "domain": "%s" }
                  ]
                }
                """.formatted(domain);

        mockMvc.perform(post("/api/v1/resume-generations")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.projects[0].rewriteAttempts", is(2)))
                .andExpect(jsonPath("$.projects[0].duplicatePhraseDetected", is(false)))
                .andExpect(jsonPath("$.projects[0].description", not(containsString("across many services"))));
    }

    private void seedKnowledgeFragment(String domain) throws Exception {
        // role must match the generation request's role - RetrievalFilter's native
        // query only skips the role predicate when the filter's role is null, and
        // ResumeGenerationService always passes a non-null role (falls back to
        // primaryRole when the project input doesn't specify one).
        String body = """
                {"fragmentType": "PROJECT_SUMMARY", "content": "%s", "domain": "%s", "role": "Java Full Stack Developer"}
                """.formatted(DUPLICATE_CONTENT, domain);
        mockMvc.perform(post("/api/v1/knowledge-fragments")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated());
    }
}

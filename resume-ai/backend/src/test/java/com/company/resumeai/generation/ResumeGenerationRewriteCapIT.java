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

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * §19: "Limit rewrites to avoid infinite loops." A fake LlmClient that
 * always reproduces the reference material verbatim (never resolves) must
 * still stop at MAX_REWRITE_ATTEMPTS and return the last draft, not loop
 * forever or error out.
 */
@Import({ResumeGenerationRewriteCapIT.FakeEmbeddingConfig.class, ResumeGenerationRewriteCapIT.AlwaysDuplicatingLlmConfig.class})
class ResumeGenerationRewriteCapIT extends AbstractIntegrationTest {

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
    static class AlwaysDuplicatingLlmConfig {
        @Bean
        @Primary
        LlmClient alwaysDuplicatingLlmClient() {
            return request -> {
                boolean isProjectPrompt = request.systemPrompt().contains("JSON object");
                if (!isProjectPrompt) {
                    return new LlmResponse("A candidate summary.", "fake-model");
                }
                return new LlmResponse("""
                        {"description": "%s", "responsibilities": ["Did one thing"], "environment": ["Java 8"]}
                        """.formatted(DUPLICATE_CONTENT), "fake-model");
            };
        }
    }

    @Test
    void stopsAtMaxRewriteAttemptsWhenNeverResolved() throws Exception {
        String domain = "RewriteCapTest-" + UUID.randomUUID();
        seedKnowledgeFragment(domain);

        String requestBody = """
                {
                  "candidateName": "Candidate A",
                  "primaryRole": "Java Full Stack Developer",
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
                .andExpect(jsonPath("$.projects[0].rewriteAttempts", is(3)))
                .andExpect(jsonPath("$.projects[0].duplicatePhraseDetected", is(true)))
                .andExpect(jsonPath("$.projects[0].similarityVerdict", is("REWRITE")));
    }

    private void seedKnowledgeFragment(String domain) throws Exception {
        // role must match the generation request's role - see the identical note in
        // ResumeGenerationRewriteIT.
        String body = """
                {"fragmentType": "PROJECT_SUMMARY", "content": "%s", "domain": "%s", "role": "Java Full Stack Developer"}
                """.formatted(DUPLICATE_CONTENT, domain);
        mockMvc.perform(post("/api/v1/knowledge-fragments")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated());
    }
}

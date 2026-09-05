package com.company.resumeai.generation;

import com.company.resumeai.AbstractIntegrationTest;
import com.company.resumeai.embedding.EmbeddingClient;
import com.company.resumeai.embedding.EmbeddingGenerationException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression test for a real bug found by manually running the app without
 * OPENAI_API_KEY set (every other *IT test's fake EmbeddingClient never
 * throws, so nothing had ever exercised this path): retrievalService.retrieveSimilar
 * runs inside its own @Transactional(readOnly = true) on a *different* bean
 * (RetrievalService, not ResumeGenerationService). When embed() threw there,
 * Spring marked the shared physical transaction rollback-only the instant the
 * exception crossed that proxy boundary - before ResumeGenerationService.generate()'s
 * own catch block ever ran - so the later resumeGenerationRepository.save(generation)
 * failed with UnexpectedRollbackException, surfacing as an opaque 500 instead of
 * a FAILED generation. Fixed by removing @Transactional from generate() itself
 * (see its javadoc) - this test pins that behavior down.
 */
@Import(ResumeGenerationEmbeddingFailureIT.FailingEmbeddingConfig.class)
class ResumeGenerationEmbeddingFailureIT extends AbstractIntegrationTest {

    @TestConfiguration
    static class FailingEmbeddingConfig {
        @Bean
        @Primary
        EmbeddingClient failingEmbeddingClient() {
            return text -> {
                throw new EmbeddingGenerationException("simulated embedding failure");
            };
        }
    }

    @Test
    void generationFailsGracefullyRatherThanReturning500WhenEmbeddingCallThrows() throws Exception {
        String requestBody = """
                {
                  "candidateName": "Candidate A",
                  "primaryRole": "Java Full Stack Developer",
                  "projects": [
                    { "client": "AT&T", "startDate": "2016-01-01", "endDate": "2019-06-30" }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/resume-generations")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.failureReason", containsString("simulated embedding failure")));
    }
}

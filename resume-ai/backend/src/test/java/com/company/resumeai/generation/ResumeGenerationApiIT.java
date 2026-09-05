package com.company.resumeai.generation;

import com.company.resumeai.AbstractIntegrationTest;
import com.company.resumeai.embedding.EmbeddingClient;
import com.company.resumeai.llm.LlmClient;
import com.company.resumeai.llm.LlmResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.Random;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Uses fake EmbeddingClient (same deterministic approach as
 * KnowledgeFragmentApiIT - retrieval still runs, it just has no real
 * fragments to rank meaningfully here) and a fake LlmClient that returns a
 * canned JSON project response or a canned summary sentence depending on
 * which prompt it was handed - no OPENAI_API_KEY needed, no real API cost.
 */
@Import({ResumeGenerationApiIT.FakeEmbeddingConfig.class, ResumeGenerationApiIT.FakeLlmConfig.class})
class ResumeGenerationApiIT extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

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
    static class FakeLlmConfig {
        @Bean
        @Primary
        LlmClient fakeLlmClient() {
            return request -> {
                if (request.systemPrompt().contains("JSON object")) {
                    return new LlmResponse("""
                            {"description": "Modernized a core billing platform for a telecom client.", \
                            "responsibilities": ["Implemented REST APIs", "Migrated batch jobs to Spring Batch"], \
                            "environment": ["Java 8", "Spring Boot"]}
                            """, "fake-model");
                }
                return new LlmResponse(
                        "Experienced Java engineer with a track record of delivering enterprise systems.",
                        "fake-model");
            };
        }
    }

    @Test
    void generatesResumeWithProjectAndSummary() throws Exception {
        String requestBody = """
                {
                  "candidateName": "Candidate A",
                  "primaryRole": "Java Full Stack Developer",
                  "totalExperienceYears": 12,
                  "projects": [
                    { "client": "AT&T", "startDate": "2016-01-01", "endDate": "2019-06-30", "domain": "Telecommunications" }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/resume-generations")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.generatedSummary", containsString("enterprise systems")))
                .andExpect(jsonPath("$.projects", hasSize(1)))
                .andExpect(jsonPath("$.projects[0].clientName", is("AT&T")))
                .andExpect(jsonPath("$.projects[0].description", containsString("billing platform")))
                .andExpect(jsonPath("$.projects[0].responsibilities", hasSize(2)))
                .andExpect(jsonPath("$.projects[0].environment", hasItem("Java 8")));
    }

    @Test
    void getReturnsPreviouslyCreatedGeneration() throws Exception {
        String requestBody = """
                {
                  "candidateName": "Candidate B",
                  "primaryRole": "Java Full Stack Developer",
                  "totalExperienceYears": 8,
                  "projects": [
                    { "client": "Bank of America", "startDate": "2016-02-01", "endDate": "2018-01-01" }
                  ]
                }
                """;

        String createResponse = mockMvc.perform(post("/api/v1/resume-generations")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(createResponse);
        String generationId = node.get("id").asText();

        mockMvc.perform(get("/api/v1/resume-generations/" + generationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateName", is("Candidate B")))
                .andExpect(jsonPath("$.projects[0].clientName", is("Bank of America")));
    }

    @Test
    void rejectsChronologicallyInvalidProject() throws Exception {
        // §29 Chronology Validation - same InvalidRequestException -> 400 mapping as
        // CandidateProjectService, not a FAILED generation: this is a bad request, not
        // an LLM/provider failure, so nothing should even be persisted.
        String requestBody = """
                {
                  "candidateName": "Candidate C",
                  "primaryRole": "Java Full Stack Developer",
                  "projects": [
                    { "client": "CVS Health", "startDate": "2020-01-01", "endDate": "2019-01-01" }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/resume-generations")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")));
    }
}

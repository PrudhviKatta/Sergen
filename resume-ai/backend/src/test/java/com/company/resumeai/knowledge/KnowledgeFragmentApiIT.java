package com.company.resumeai.knowledge;

import com.company.resumeai.AbstractIntegrationTest;
import com.company.resumeai.embedding.EmbeddingClient;
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
 * Uses a fake, deterministic EmbeddingClient (same text -> same vector,
 * different text -> effectively-random vector) instead of a real OpenAI call
 * - no API key needed, works in CI, and "identical text ranks first" is
 * enough to prove the pgvector round-trip and cosine-distance ordering work,
 * without needing real semantic embeddings.
 */
@Import(KnowledgeFragmentApiIT.FakeEmbeddingConfig.class)
class KnowledgeFragmentApiIT extends AbstractIntegrationTest {

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

    @Test
    void createsFragmentWithEmbedding() throws Exception {
        mockMvc.perform(post("/api/v1/knowledge-fragments")
                        .contentType("application/json")
                        .content("""
                                {"fragmentType": "PROJECT_SUMMARY", "content": "Built a payments platform", "domain": "Banking"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hasEmbedding", is(true)))
                .andExpect(jsonPath("$.domain", is("Banking")));
    }

    @Test
    void searchRanksIdenticalContentFirst() throws Exception {
        String targetContent = "Modernized a core banking ledger system";
        String targetId = createFragment(targetContent, "Banking");
        createFragment("Unrelated telecom billing project", "Telecom");

        mockMvc.perform(get("/api/v1/knowledge-fragments/search")
                        .param("query", targetContent)
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(targetId)));
    }

    @Test
    void searchFiltersByDomain() throws Exception {
        // AbstractIntegrationTest shares one DB across the whole suite (no per-test
        // rollback, by design - see its javadoc), so domain values must be unique to
        // this test to avoid colliding with fragments other tests/methods left behind.
        String bankingDomain = "Banking-" + java.util.UUID.randomUUID();
        String telecomDomain = "Telecom-" + java.util.UUID.randomUUID();
        createFragment("Payments system A", bankingDomain);
        createFragment("Telecom billing system", telecomDomain);

        mockMvc.perform(get("/api/v1/knowledge-fragments/search")
                        .param("query", "some system")
                        .param("domain", bankingDomain))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].domain", is(bankingDomain)));
    }

    private String createFragment(String content, String domain) throws Exception {
        String body = """
                {"fragmentType": "PROJECT_SUMMARY", "content": "%s", "domain": "%s"}
                """.formatted(content, domain);
        String response = mockMvc.perform(post("/api/v1/knowledge-fragments")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return node.get("id").asText();
    }
}

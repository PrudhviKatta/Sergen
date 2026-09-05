package com.company.resumeai.ingestion;

import com.company.resumeai.AbstractIntegrationTest;
import com.company.resumeai.embedding.EmbeddingClient;
import com.company.resumeai.knowledge.KnowledgeFragmentRepository;
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
import org.springframework.mock.web.MockMultipartFile;

import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fake EmbeddingClient (same deterministic approach as KnowledgeFragmentApiIT)
 * and a fake LlmClient returning a canned §10-shaped JSON response - proves
 * the whole §9 pipeline (extract -> parse -> persist -> create knowledge
 * fragments + embeddings) without a real API key or cost. A unique domain
 * value per fake response avoids colliding with fragments other tests leave
 * behind in this suite's shared database (see AbstractIntegrationTest).
 */
@Import({ResumeUploadApiIT.FakeEmbeddingConfig.class, ResumeUploadApiIT.FakeLlmConfig.class})
class ResumeUploadApiIT extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KnowledgeFragmentRepository knowledgeFragmentRepository;

    @TestConfiguration
    static class FakeEmbeddingConfig {
        @Bean
        @Primary
        EmbeddingClient fakeEmbeddingClient() {
            return text -> {
                Random random = new Random(text.hashCode());
                float[] vector = new float[1536];
                for (int i = 0; i < vector.length; i++) {
                    vector[i] = random.nextFloat() * 2f - 1f;
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
            return request -> new LlmResponse("""
                    {"candidate": {"firstName": "Jane", "lastName": "Doe", "email": "jane@example.com",
                     "primaryRole": "Java Full Stack Developer", "totalExperience": 9,
                     "summary": "Experienced backend engineer.",
                     "technicalSkills": ["Java", "Kubernetes"],
                     "education": ["B.S. Computer Science, State University, 2013"],
                     "certifications": ["AWS Certified Solutions Architect, 2021"]},
                     "projects": [{"client": "AT&T", "role": "Java Developer", "startDate": "2016-01",
                     "endDate": "2019-06", "domain": "Telecommunications-%s",
                     "technologies": ["Java 8", "Spring Boot"],
                     "responsibilities": ["Built REST APIs", "Migrated legacy batch jobs"]}]}
                    """.formatted(UUID.randomUUID()), "fake-model");
        }
    }

    @Test
    void uploadsPlainTextResumeAndParsesIt() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.txt", "text/plain",
                "Jane Doe - Java Full Stack Developer, 9 years experience".getBytes());

        mockMvc.perform(multipart("/api/v1/resumes/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PARSED")))
                .andExpect(jsonPath("$.fileType", is("txt")))
                .andExpect(jsonPath("$.rawText", containsString("Jane Doe")))
                .andExpect(jsonPath("$.parsedJson.candidate.primaryRole", is("Java Full Stack Developer")))
                .andExpect(jsonPath("$.parsedJson.candidate.email", is("jane@example.com")))
                .andExpect(jsonPath("$.parsedJson.candidate.technicalSkills", hasItem("Kubernetes")))
                .andExpect(jsonPath("$.parsedJson.candidate.education",
                        hasItem("B.S. Computer Science, State University, 2013")))
                .andExpect(jsonPath("$.parsedJson.candidate.certifications",
                        hasItem("AWS Certified Solutions Architect, 2021")))
                .andExpect(jsonPath("$.parsedJson.projects[0].client", is("AT&T")));
    }

    @Test
    void createsAggregateSkillsFragmentFromCandidateLevelTechnicalSkills() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.txt", "text/plain", "Jane Doe resume text".getBytes());

        mockMvc.perform(multipart("/api/v1/resumes/upload").file(file))
                .andExpect(status().isCreated());

        // Fake embeddings are unrelated to actual text meaning (deterministic per exact
        // string only), so ranking within this suite's other fragments is unpredictable -
        // a large limit is what actually guarantees this fragment is present in the
        // results, not the query wording.
        mockMvc.perform(get("/api/v1/knowledge-fragments/search")
                        .param("query", "Java Kubernetes skills")
                        .param("limit", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.content == 'Overall technical skills: Java, Kubernetes')]").exists());
    }

    @Test
    void getReturnsPreviouslyUploadedResume() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.txt", "text/plain", "Some resume text".getBytes());

        String createResponse = mockMvc.perform(multipart("/api/v1/resumes/upload").file(file))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(createResponse);

        mockMvc.perform(get("/api/v1/resumes/" + node.get("id").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PARSED")));
    }

    @Test
    void deleteRemovesResumeSourceAndItsKnowledgeFragments() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.txt", "text/plain",
                "Jane Doe resume text with real project content".getBytes());

        String createResponse = mockMvc.perform(multipart("/api/v1/resumes/upload").file(file))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID resumeId = UUID.fromString(objectMapper.readTree(createResponse).get("id").asText());

        // Fragments were actually created for this resume before we delete it -
        // otherwise "zero fragments left" after delete would be true trivially.
        assertThat(knowledgeFragmentRepository.findAll().stream()
                .filter(f -> resumeId.equals(f.getSourceResumeId())))
                .isNotEmpty();

        mockMvc.perform(delete("/api/v1/resumes/" + resumeId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/resumes/" + resumeId))
                .andExpect(status().isNotFound());
        assertThat(knowledgeFragmentRepository.findAll().stream()
                .filter(f -> resumeId.equals(f.getSourceResumeId())))
                .isEmpty();
    }

    @Test
    void deletingUnknownResumeReturns404() throws Exception {
        mockMvc.perform(delete("/api/v1/resumes/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsUnsupportedFileType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.rtf", "application/rtf", "some content".getBytes());

        mockMvc.perform(multipart("/api/v1/resumes/upload").file(file))
                .andExpect(status().isBadRequest());
    }
}

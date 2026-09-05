package com.company.resumeai.ingestion;

import com.company.resumeai.AbstractIntegrationTest;
import com.company.resumeai.llm.LlmClient;
import com.company.resumeai.llm.LlmResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Same regression class as generation.ResumeGenerationEmbeddingFailureIT: a
 * malformed LLM response (or a missing API key) during parsing must produce
 * a FAILED resume_source with the raw text still preserved - not a 500, and
 * not a silently-lost upload.
 */
@Import(ResumeUploadParsingFailureIT.UnparseableLlmConfig.class)
class ResumeUploadParsingFailureIT extends AbstractIntegrationTest {

    @TestConfiguration
    static class UnparseableLlmConfig {
        @Bean
        @Primary
        LlmClient unparseableLlmClient() {
            return request -> new LlmResponse("this is not valid JSON at all", "fake-model");
        }
    }

    @Test
    void uploadFailsGracefullyButPreservesRawTextWhenParsingFails() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.txt", "text/plain", "Some resume text that will fail to parse".getBytes());

        mockMvc.perform(multipart("/api/v1/resumes/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.rawText", containsString("Some resume text that will fail to parse")))
                .andExpect(jsonPath("$.parsedJson", nullValue()))
                .andExpect(jsonPath("$.failureReason", containsString("Could not parse")));
    }
}

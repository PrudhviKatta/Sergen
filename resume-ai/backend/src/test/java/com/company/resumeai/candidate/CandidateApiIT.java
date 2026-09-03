package com.company.resumeai.candidate;

import com.company.resumeai.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CandidateApiIT extends AbstractIntegrationTest {

    @Test
    void createAndFetchCandidate() throws Exception {
        String requestBody = """
                {
                  "firstName": "Ada",
                  "lastName": "Lovelace",
                  "email": "ada@example.com",
                  "primaryRole": "Java Full Stack Developer",
                  "totalExperienceYears": 12
                }
                """;

        String location = mockMvc.perform(post("/api/v1/candidates")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("ada@example.com")))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Ada")));
    }

    @Test
    void rejectsDuplicateEmail() throws Exception {
        String requestBody = """
                {"firstName":"Grace","lastName":"Hopper","email":"grace@example.com"}
                """;

        mockMvc.perform(post("/api/v1/candidates").contentType("application/json").content(requestBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/candidates").contentType("application/json").content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")));
    }

    @Test
    void rejectsMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/v1/candidates")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_FAILED")));
    }

    @Test
    void returns404ForUnknownCandidate() throws Exception {
        mockMvc.perform(get("/api/v1/candidates/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}

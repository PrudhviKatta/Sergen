package com.company.resumeai.project;

import com.company.resumeai.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CandidateProjectApiIT extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createProjectUnderCandidate() throws Exception {
        String candidateId = createCandidate("proj.candidate@example.com");
        String clientId = createClient("Bank of America");

        String requestBody = """
                {
                  "clientId": "%s",
                  "projectName": "Core Banking Modernization",
                  "roleTitle": "Java Full Stack Developer",
                  "startDate": "2014-02-01",
                  "endDate": "2016-07-31",
                  "domain": "Banking"
                }
                """.formatted(clientId);

        mockMvc.perform(post("/api/v1/candidates/" + candidateId + "/projects")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientName", is("Bank of America")));

        mockMvc.perform(get("/api/v1/candidates/" + candidateId + "/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void rejectsEndDateBeforeStartDate() throws Exception {
        String candidateId = createCandidate("bad.dates@example.com");
        String clientId = createClient("AT&T");

        String requestBody = """
                {"clientId": "%s", "startDate": "2020-01-01", "endDate": "2019-01-01"}
                """.formatted(clientId);

        mockMvc.perform(post("/api/v1/candidates/" + candidateId + "/projects")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")));
    }

    @Test
    void returns404ForUnknownClient() throws Exception {
        String candidateId = createCandidate("unknown.client@example.com");

        String requestBody = """
                {"clientId": "%s", "startDate": "2020-01-01", "endDate": "2021-01-01"}
                """.formatted(java.util.UUID.randomUUID());

        mockMvc.perform(post("/api/v1/candidates/" + candidateId + "/projects")
                        .contentType("application/json")
                        .content(requestBody))
                .andExpect(status().isNotFound());
    }

    private String createCandidate(String email) throws Exception {
        String body = """
                {"firstName": "Test", "lastName": "Candidate", "email": "%s"}
                """.formatted(email);
        String response = mockMvc.perform(post("/api/v1/candidates").contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return readId(response);
    }

    private String createClient(String name) throws Exception {
        String body = """
                {"name": "%s"}
                """.formatted(name);
        String response = mockMvc.perform(post("/api/v1/clients").contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return readId(response);
    }

    private String readId(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        return node.get("id").asText();
    }
}

package com.company.resumeai.client;

import com.company.resumeai.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClientApiIT extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createClient() throws Exception {
        mockMvc.perform(post("/api/v1/clients")
                        .contentType("application/json")
                        .content("""
                                {"name": "AT&T", "industry": "Telecommunications"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("AT&T")))
                .andExpect(jsonPath("$.normalizedName", is("AT&T")));
    }

    @Test
    void reusesExistingClientOnDuplicateName() throws Exception {
        String body = """
                {"name": "  jpmorgan chase  "}
                """;

        String firstResponse = mockMvc.perform(post("/api/v1/clients").contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String secondResponse = mockMvc.perform(post("/api/v1/clients").contentType("application/json")
                        .content("""
                                {"name": "JPMORGAN CHASE"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String firstId = objectMapper.readTree(firstResponse).get("id").asText();
        String secondId = objectMapper.readTree(secondResponse).get("id").asText();
        assertThat(firstId).isEqualTo(secondId);
    }
}

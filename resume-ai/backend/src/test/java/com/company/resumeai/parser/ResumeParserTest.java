package com.company.resumeai.parser;

import com.company.resumeai.llm.LlmClient;
import com.company.resumeai.llm.LlmResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResumeParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesCandidateAndProjectsFromLlmJsonResponse() {
        LlmClient fakeLlmClient = request -> new LlmResponse("""
                {"candidate": {"primaryRole": "Java Full Stack Developer", "totalExperience": 11},
                 "projects": [{"client": "AT&T", "role": "Java Developer", "startDate": "2015-01",
                 "endDate": "2017-06", "domain": "Telecommunications",
                 "technologies": ["Java 8", "Spring MVC"], "responsibilities": ["Built APIs", "Fixed bugs"]}]}
                """, "fake-model");
        ResumeParser parser = new ResumeParser(fakeLlmClient, objectMapper);

        ParsedResume result = parser.parse("some raw resume text");

        assertThat(result.candidate().primaryRole()).isEqualTo("Java Full Stack Developer");
        assertThat(result.candidate().totalExperienceYears()).isEqualTo(11);
        assertThat(result.projects()).hasSize(1);
        ParsedProject project = result.projects().get(0);
        assertThat(project.client()).isEqualTo("AT&T");
        assertThat(project.startDate()).isEqualTo("2015-01");
        assertThat(project.technologies()).containsExactly("Java 8", "Spring MVC");
        assertThat(project.responsibilities()).containsExactly("Built APIs", "Fixed bugs");
    }

    @Test
    void parsesNonProjectCandidateSections() {
        LlmClient fakeLlmClient = request -> new LlmResponse("""
                {"candidate": {"firstName": "Jane", "lastName": "Doe", "email": "jane@example.com",
                 "phone": "555-1234", "location": "Austin, TX", "primaryRole": "Java Developer",
                 "totalExperience": 9, "summary": "Experienced backend engineer.",
                 "technicalSkills": ["Java", "Kubernetes"],
                 "education": ["B.S. Computer Science, State University, 2013"],
                 "certifications": ["AWS Certified Solutions Architect, 2021"]},
                 "projects": []}
                """, "fake-model");
        ResumeParser parser = new ResumeParser(fakeLlmClient, objectMapper);

        ParsedCandidate candidate = parser.parse("some raw resume text").candidate();

        assertThat(candidate.firstName()).isEqualTo("Jane");
        assertThat(candidate.lastName()).isEqualTo("Doe");
        assertThat(candidate.email()).isEqualTo("jane@example.com");
        assertThat(candidate.phone()).isEqualTo("555-1234");
        assertThat(candidate.location()).isEqualTo("Austin, TX");
        assertThat(candidate.summary()).isEqualTo("Experienced backend engineer.");
        assertThat(candidate.technicalSkills()).containsExactly("Java", "Kubernetes");
        assertThat(candidate.education()).containsExactly("B.S. Computer Science, State University, 2013");
        assertThat(candidate.certifications()).containsExactly("AWS Certified Solutions Architect, 2021");
    }

    @Test
    void returnsEmptyProjectsWhenNoneIdentified() {
        LlmClient fakeLlmClient = request -> new LlmResponse(
                "{\"candidate\": {\"primaryRole\": \"Developer\"}, \"projects\": []}", "fake-model");
        ResumeParser parser = new ResumeParser(fakeLlmClient, objectMapper);

        ParsedResume result = parser.parse("short text");

        assertThat(result.projects()).isEmpty();
        assertThat(result.candidate().totalExperienceYears()).isNull();
        assertThat(result.candidate().technicalSkills()).isEmpty();
        assertThat(result.candidate().education()).isEmpty();
        assertThat(result.candidate().certifications()).isEmpty();
    }

    @Test
    void throwsWhenResponseIsNotValidJson() {
        LlmClient fakeLlmClient = request -> new LlmResponse("not json at all", "fake-model");
        ResumeParser parser = new ResumeParser(fakeLlmClient, objectMapper);

        assertThatThrownBy(() -> parser.parse("text"))
                .isInstanceOf(ResumeParsingException.class);
    }
}

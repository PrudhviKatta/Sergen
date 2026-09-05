package com.company.resumeai.prompt;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectGenerationPromptBuilderTest {

    @Test
    void systemPromptForbidsFabricationAndRequestsJson() {
        PromptMessages messages = ProjectGenerationPromptBuilder.build(sampleContext());

        assertThat(messages.system()).contains("Do not invent");
        assertThat(messages.system()).contains("Do not claim technologies outside the approved list");
        assertThat(messages.system()).contains("JSON object");
    }

    @Test
    void userPromptIncludesClientTimelineRoleAndApprovedTechnologies() {
        PromptMessages messages = ProjectGenerationPromptBuilder.build(sampleContext());

        assertThat(messages.user()).contains("Client: AT&T");
        assertThat(messages.user()).contains("2016-01-01 to 2019-06-30");
        assertThat(messages.user()).contains("Role: Java Full Stack Developer");
        assertThat(messages.user()).contains("DOMAIN CONTEXT:\nTelecommunications");
        assertThat(messages.user()).contains("Java 8");
        assertThat(messages.user()).contains("Spring Boot");
        assertThat(messages.user()).contains("REFERENCE PROJECT PATTERNS:");
        assertThat(messages.user()).contains("- Modernized a billing platform");
    }

    @Test
    void omitsDomainAndReferenceSectionsWhenNotProvided() {
        ProjectGenerationContext context = new ProjectGenerationContext(
                "Java Full Stack Developer", "AT&T", LocalDate.of(2016, 1, 1), LocalDate.of(2019, 6, 30),
                null, List.of("Java 8"), List.of());

        PromptMessages messages = ProjectGenerationPromptBuilder.build(context);

        assertThat(messages.user()).doesNotContain("DOMAIN CONTEXT");
        assertThat(messages.user()).doesNotContain("REFERENCE PROJECT PATTERNS");
    }

    private ProjectGenerationContext sampleContext() {
        return new ProjectGenerationContext(
                "Java Full Stack Developer",
                "AT&T",
                LocalDate.of(2016, 1, 1),
                LocalDate.of(2019, 6, 30),
                "Telecommunications",
                List.of("Java 8", "Spring Boot"),
                List.of("Modernized a billing platform"));
    }
}

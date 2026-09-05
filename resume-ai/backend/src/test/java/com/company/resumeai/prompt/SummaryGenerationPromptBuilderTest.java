package com.company.resumeai.prompt;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SummaryGenerationPromptBuilderTest {

    @Test
    void systemPromptForbidsFabricationAndRequestsPlainText() {
        PromptMessages messages = SummaryGenerationPromptBuilder.build(sampleContext());

        assertThat(messages.system()).contains("Do not invent employers, certifications");
        assertThat(messages.system()).contains("plain text only");
    }

    @Test
    void userPromptListsCandidateAndEveryProjectDescription() {
        PromptMessages messages = SummaryGenerationPromptBuilder.build(sampleContext());

        assertThat(messages.user()).contains("Candidate: Candidate A");
        assertThat(messages.user()).contains("Primary role: Java Full Stack Developer");
        assertThat(messages.user()).contains("Total experience: 12 years");
        assertThat(messages.user()).contains("1. Modernized a payments platform");
        assertThat(messages.user()).contains("2. Rebuilt a billing system");
    }

    @Test
    void omitsExperienceLineWhenNotProvided() {
        SummaryGenerationContext context = new SummaryGenerationContext(
                "Candidate A", "Java Full Stack Developer", null, List.of("Did some work"));

        PromptMessages messages = SummaryGenerationPromptBuilder.build(context);

        assertThat(messages.user()).doesNotContain("Total experience");
    }

    private SummaryGenerationContext sampleContext() {
        return new SummaryGenerationContext(
                "Candidate A",
                "Java Full Stack Developer",
                12,
                List.of("Modernized a payments platform", "Rebuilt a billing system"));
    }
}

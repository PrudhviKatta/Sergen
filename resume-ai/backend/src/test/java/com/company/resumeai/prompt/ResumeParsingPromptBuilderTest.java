package com.company.resumeai.prompt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResumeParsingPromptBuilderTest {

    @Test
    void systemPromptForbidsInventingDataAndRequestsJson() {
        PromptMessages messages = ResumeParsingPromptBuilder.build("some resume text");

        assertThat(messages.system()).contains("do not infer, guess, or invent");
        assertThat(messages.system()).contains("JSON object");
    }

    @Test
    void userPromptIncludesRawResumeText() {
        PromptMessages messages = ResumeParsingPromptBuilder.build("Jane Doe, Java Developer at AT&T");

        assertThat(messages.user()).contains("Jane Doe, Java Developer at AT&T");
    }
}

package com.company.resumeai.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure logic (request building, response parsing, missing-key handling) -
 * no network call, same approach as embedding.OpenAiEmbeddingClientTest.
 */
class OpenAiLlmClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void generateThrowsWhenApiKeyNotConfigured() {
        OpenAiLlmClient client = new OpenAiLlmClient("", "gpt-4o-mini", objectMapper);

        assertThatThrownBy(() -> client.generate(new LlmRequest("system", "user")))
                .isInstanceOf(LlmGenerationException.class)
                .hasMessageContaining("OPENAI_API_KEY");
    }

    @Test
    void buildsRequestBodyWithModelAndMessages() {
        OpenAiLlmClient client = new OpenAiLlmClient("test-key", "gpt-4o-mini", objectMapper);

        String body = client.buildRequestBody(new LlmRequest("Be concise.", "Describe a project."), "gpt-4o-mini");

        assertThat(body).contains("\"model\":\"gpt-4o-mini\"");
        assertThat(body).contains("\"role\":\"system\"");
        assertThat(body).contains("\"content\":\"Be concise.\"");
        assertThat(body).contains("\"role\":\"user\"");
        assertThat(body).contains("\"content\":\"Describe a project.\"");
    }

    @Test
    void parsesContentFromChatCompletionResponse() {
        OpenAiLlmClient client = new OpenAiLlmClient("test-key", "gpt-4o-mini", objectMapper);
        String json = """
                {
                  "model": "gpt-4o-mini",
                  "choices": [ { "message": { "role": "assistant", "content": "Hello there" } } ]
                }
                """;

        LlmResponse response = client.parseChatResponse(json);

        assertThat(response.content()).isEqualTo("Hello there");
        assertThat(response.model()).isEqualTo("gpt-4o-mini");
    }

    @Test
    void throwsWhenResponseHasNoChoices() {
        OpenAiLlmClient client = new OpenAiLlmClient("test-key", "gpt-4o-mini", objectMapper);

        assertThatThrownBy(() -> client.parseChatResponse("{\"choices\": []}"))
                .isInstanceOf(LlmGenerationException.class);
    }
}

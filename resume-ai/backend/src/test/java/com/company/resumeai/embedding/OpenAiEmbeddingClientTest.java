package com.company.resumeai.embedding;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure logic (request building, response parsing, missing-key handling) -
 * no network call. See docs/IMPLEMENTATION_NOTES.md for how to exercise a
 * real live call once an OPENAI_API_KEY is available.
 */
class OpenAiEmbeddingClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void embedThrowsWhenApiKeyNotConfigured() {
        OpenAiEmbeddingClient client = new OpenAiEmbeddingClient("", "text-embedding-3-small", objectMapper);

        assertThatThrownBy(() -> client.embed("some text"))
                .isInstanceOf(EmbeddingGenerationException.class)
                .hasMessageContaining("OPENAI_API_KEY");
    }

    @Test
    void buildsRequestBodyWithModelAndInput() {
        OpenAiEmbeddingClient client = new OpenAiEmbeddingClient("test-key", "text-embedding-3-small", objectMapper);

        String body = client.buildRequestBody("Built a payments platform", "text-embedding-3-small");

        assertThat(body).contains("\"model\":\"text-embedding-3-small\"");
        assertThat(body).contains("\"input\":\"Built a payments platform\"");
    }

    @Test
    void parsesEmbeddingFromResponseJson() {
        OpenAiEmbeddingClient client = new OpenAiEmbeddingClient("test-key", "text-embedding-3-small", objectMapper);
        String json = """
                {
                  "data": [ { "embedding": [0.1, 0.2, 0.3], "index": 0 } ],
                  "model": "text-embedding-3-small"
                }
                """;

        float[] result = client.parseEmbeddingResponse(json);

        assertThat(result).containsExactly(0.1f, 0.2f, 0.3f);
    }

    @Test
    void throwsWhenResponseHasNoEmbeddingData() {
        OpenAiEmbeddingClient client = new OpenAiEmbeddingClient("test-key", "text-embedding-3-small", objectMapper);

        assertThatThrownBy(() -> client.parseEmbeddingResponse("{\"data\": []}"))
                .isInstanceOf(EmbeddingGenerationException.class);
    }
}

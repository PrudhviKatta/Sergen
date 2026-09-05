package com.company.resumeai.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * §26/§49: calls OpenAI's chat completions endpoint directly over
 * {@link java.net.http.HttpClient}, same pattern as
 * embedding.OpenAiEmbeddingClient - no SDK dependency, Jackson is already on
 * the classpath. Fails at call time with LlmGenerationException if no API
 * key is configured, not at startup.
 */
@Component
public class OpenAiLlmClient implements LlmClient {

    private static final URI CHAT_COMPLETIONS_URL = URI.create("https://api.openai.com/v1/chat/completions");

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiLlmClient(@Value("${openai.api-key:}") String apiKey,
                            @Value("${openai.chat-model:gpt-4o-mini}") String model,
                            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new LlmGenerationException(
                    "OPENAI_API_KEY is not configured - set it as an environment variable before calling generate()");
        }

        HttpRequest httpRequest = HttpRequest.newBuilder(CHAT_COMPLETIONS_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(request, model)))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new LlmGenerationException("Failed to call OpenAI chat completions API", e);
        }

        if (response.statusCode() != 200) {
            throw new LlmGenerationException(
                    "OpenAI chat completions API returned HTTP " + response.statusCode() + ": " + response.body());
        }

        return parseChatResponse(response.body());
    }

    String buildRequestBody(LlmRequest request, String model) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        ArrayNode messages = body.putArray("messages");
        messages.addObject().put("role", "system").put("content", request.systemPrompt());
        messages.addObject().put("role", "user").put("content", request.userPrompt());
        return body.toString();
    }

    LlmResponse parseChatResponse(String json) {
        JsonNode root;
        JsonNode contentNode;
        try {
            root = objectMapper.readTree(json);
            contentNode = root.at("/choices/0/message/content");
        } catch (IOException e) {
            throw new LlmGenerationException("Could not parse OpenAI chat completions response: " + json, e);
        }
        if (contentNode.isMissingNode() || !contentNode.isTextual()) {
            throw new LlmGenerationException("OpenAI chat completions response had no message content: " + json);
        }
        String responseModel = root.path("model").asText(model);
        return new LlmResponse(contentNode.asText(), responseModel);
    }
}

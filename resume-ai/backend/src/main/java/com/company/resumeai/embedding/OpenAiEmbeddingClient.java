package com.company.resumeai.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * §26/§49: calls OpenAI's embeddings endpoint directly over
 * {@link java.net.http.HttpClient} - no SDK dependency, since this is one
 * endpoint and Jackson (for request/response JSON) is already on the
 * classpath via spring-boot-starter-web. text-embedding-3-small was chosen
 * for its 1536-dimension output, which is what V1__init_schema.sql already
 * declared as a placeholder for knowledge_fragment.embedding - see
 * IMPLEMENTATION_NOTES.md for the cost/quality tradeoff against
 * text-embedding-3-large.
 *
 * Fails at call time with EmbeddingGenerationException if no API key is
 * configured, not at startup - most of the app (candidate/client/project
 * CRUD, the technology timeline engine) has no reason to require one.
 */
@Component
public class OpenAiEmbeddingClient implements EmbeddingClient {

    private static final URI EMBEDDINGS_URL = URI.create("https://api.openai.com/v1/embeddings");

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenAiEmbeddingClient(@Value("${openai.api-key:}") String apiKey,
                                  @Value("${openai.embedding-model:text-embedding-3-small}") String model,
                                  ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public float[] embed(String text) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new EmbeddingGenerationException(
                    "OPENAI_API_KEY is not configured - set it as an environment variable before calling embed()");
        }

        HttpRequest request = HttpRequest.newBuilder(EMBEDDINGS_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(text, model)))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new EmbeddingGenerationException("Failed to call OpenAI embeddings API", e);
        }

        if (response.statusCode() != 200) {
            throw new EmbeddingGenerationException(
                    "OpenAI embeddings API returned HTTP " + response.statusCode() + ": " + response.body());
        }

        return parseEmbeddingResponse(response.body());
    }

    String buildRequestBody(String text, String model) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("input", text);
        return body.toString();
    }

    float[] parseEmbeddingResponse(String json) {
        JsonNode embeddingNode;
        try {
            JsonNode root = objectMapper.readTree(json);
            embeddingNode = root.at("/data/0/embedding");
        } catch (IOException e) {
            throw new EmbeddingGenerationException("Could not parse OpenAI embeddings response: " + json, e);
        }
        if (embeddingNode.isMissingNode() || !embeddingNode.isArray() || embeddingNode.isEmpty()) {
            throw new EmbeddingGenerationException("OpenAI embeddings response had no embedding data: " + json);
        }
        float[] vector = new float[embeddingNode.size()];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) embeddingNode.get(i).asDouble();
        }
        return vector;
    }
}

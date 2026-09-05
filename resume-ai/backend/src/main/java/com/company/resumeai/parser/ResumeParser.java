package com.company.resumeai.parser;

import com.company.resumeai.llm.LlmClient;
import com.company.resumeai.llm.LlmRequest;
import com.company.resumeai.llm.LlmResponse;
import com.company.resumeai.prompt.PromptMessages;
import com.company.resumeai.prompt.ResumeParsingPromptBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * §10 resume parser: turns raw extracted text into ParsedResume via the LLM
 * (see ResumeParsingPromptBuilder for why an LLM, not rule-based parsing -
 * resume layouts vary too much for Phase 1 to hand-write section detection).
 * Manual JsonNode-based parsing rather than objectMapper.readValue(...,
 * ParsedResume.class), same approach as generation.ResumeGenerationService's
 * project-JSON parsing - tolerates a field being missing/null instead of
 * failing the whole parse, and doesn't depend on Jackson's record
 * deserialization support being configured for this project.
 */
@Service
public class ResumeParser {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public ResumeParser(LlmClient llmClient, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    public ParsedResume parse(String resumeText) {
        PromptMessages prompt = ResumeParsingPromptBuilder.build(resumeText);
        LlmResponse response = llmClient.generate(new LlmRequest(prompt.system(), prompt.user()));
        return parseJson(response.content());
    }

    private ParsedResume parseJson(String content) {
        JsonNode root;
        try {
            root = objectMapper.readTree(content);
        } catch (IOException e) {
            throw new ResumeParsingException("Could not parse LLM resume-parsing response: " + content, e);
        }

        JsonNode candidateNode = root.path("candidate");
        ParsedCandidate candidate = new ParsedCandidate(
                textOrNull(candidateNode, "firstName"),
                textOrNull(candidateNode, "lastName"),
                textOrNull(candidateNode, "email"),
                textOrNull(candidateNode, "phone"),
                textOrNull(candidateNode, "location"),
                textOrNull(candidateNode, "primaryRole"),
                intOrNull(candidateNode, "totalExperience"),
                textOrNull(candidateNode, "summary"),
                toStringList(candidateNode.path("technicalSkills")),
                toStringList(candidateNode.path("education")),
                toStringList(candidateNode.path("certifications")));

        List<ParsedProject> projects = new ArrayList<>();
        for (JsonNode projectNode : root.path("projects")) {
            projects.add(new ParsedProject(
                    textOrNull(projectNode, "client"),
                    textOrNull(projectNode, "role"),
                    textOrNull(projectNode, "startDate"),
                    textOrNull(projectNode, "endDate"),
                    textOrNull(projectNode, "domain"),
                    toStringList(projectNode.path("technologies")),
                    toStringList(projectNode.path("responsibilities"))));
        }

        return new ParsedResume(candidate, projects);
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private Integer intOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asInt();
    }

    private List<String> toStringList(JsonNode arrayNode) {
        if (!arrayNode.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        arrayNode.forEach(node -> values.add(node.asText()));
        return values;
    }
}

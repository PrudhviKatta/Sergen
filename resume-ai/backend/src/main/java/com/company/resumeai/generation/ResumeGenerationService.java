package com.company.resumeai.generation;

import com.company.resumeai.common.exception.ResourceNotFoundException;
import com.company.resumeai.embedding.EmbeddingGenerationException;
import com.company.resumeai.knowledge.KnowledgeFragment;
import com.company.resumeai.llm.LlmClient;
import com.company.resumeai.llm.LlmGenerationException;
import com.company.resumeai.llm.LlmRequest;
import com.company.resumeai.llm.LlmResponse;
import com.company.resumeai.prompt.ProjectGenerationContext;
import com.company.resumeai.prompt.ProjectGenerationPromptBuilder;
import com.company.resumeai.prompt.PromptMessages;
import com.company.resumeai.prompt.SummaryGenerationContext;
import com.company.resumeai.prompt.SummaryGenerationPromptBuilder;
import com.company.resumeai.retrieval.RetrievalFilter;
import com.company.resumeai.retrieval.RetrievalService;
import com.company.resumeai.similarity.SimilarityCheckResult;
import com.company.resumeai.similarity.SimilarityValidator;
import com.company.resumeai.similarity.SimilarityVerdict;
import com.company.resumeai.validation.ChronologyValidator;
import com.company.resumeai.validation.TechnologyTimelineValidator;
import com.company.resumeai.validation.TimelineStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * §12 resume generation pipeline, the parts Milestone 5 owns: build project
 * context (retrieval + era-appropriate technologies), generate a project
 * draft, generate the candidate summary. Timeline validation is implicit -
 * the approved-technology list handed to the prompt already came from
 * TechnologyTimelineValidator, so the LLM is steered rather than checked
 * after the fact. Similarity validation and the rewrite loop (§12's next two
 * pipeline steps, §19) run per project: a draft that scores REWRITE (either
 * too semantically similar to reference material, or an outright §18
 * duplicate phrase) is regenerated, up to MAX_REWRITE_ATTEMPTS total tries,
 * before the last draft is accepted as-is (§19: "limit rewrites to avoid
 * infinite loops").
 */
@Service
public class ResumeGenerationService {

    private static final int REFERENCE_SNIPPET_LIMIT = 5;
    private static final int MAX_REWRITE_ATTEMPTS = 3;
    private static final String REWRITE_HINT =
            "\n\nThe previous draft was too similar to existing reference material. "
                    + "Rephrase with different sentence structure and wording.";

    private final ResumeGenerationRepository resumeGenerationRepository;
    private final RetrievalService retrievalService;
    private final TechnologyTimelineValidator technologyTimelineValidator;
    private final SimilarityValidator similarityValidator;
    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public ResumeGenerationService(ResumeGenerationRepository resumeGenerationRepository,
                                    RetrievalService retrievalService,
                                    TechnologyTimelineValidator technologyTimelineValidator,
                                    SimilarityValidator similarityValidator,
                                    LlmClient llmClient,
                                    ObjectMapper objectMapper) {
        this.resumeGenerationRepository = resumeGenerationRepository;
        this.retrievalService = retrievalService;
        this.technologyTimelineValidator = technologyTimelineValidator;
        this.similarityValidator = similarityValidator;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Runs the whole pipeline synchronously and stores the result either way
     * (COMPLETED or FAILED) - no queue/async infra exists yet (that's a
     * later concern, not part of §12's pipeline itself), so the request just
     * waits for the LLM calls to finish.
     *
     * Deliberately NOT @Transactional: this method calls out to slow external
     * LLM/embedding APIs, and holding a DB transaction open across those is bad
     * practice regardless - but it would also be an outright bug here, since
     * retrievalService.retrieveSimilar() runs inside its own
     * @Transactional(readOnly = true) on a different bean. If that method's
     * embed() call throws (e.g. no OPENAI_API_KEY configured), Spring marks the
     * shared physical transaction rollback-only the moment the exception crosses
     * *that* proxy boundary - before it ever reaches this method's catch block -
     * so the later resumeGenerationRepository.save(generation) below would fail
     * with UnexpectedRollbackException even though the exception was caught.
     * save() manages its own transaction via Spring Data's repository proxy, so
     * nothing here needs one anyway.
     */
    public ResumeGeneration generate(ResumeGenerationRequest request) {
        ResumeGeneration generation = new ResumeGeneration(
                request.candidateName(), request.primaryRole(), request.totalExperienceYears());

        try {
            List<String> projectDescriptions = new ArrayList<>();
            for (ProjectGenerationInput input : request.projects()) {
                // §18 "resumes from the same candidate": the other projects already
                // drafted in this same request are compared against too, so one
                // candidate's own projects don't end up reading identically.
                GeneratedProject project = generateProject(request.primaryRole(), input, projectDescriptions);
                generation.addProject(project);
                projectDescriptions.add(project.getDescription());
            }

            LlmResponse summaryResponse = llmClient.generate(buildSummaryPrompt(request, projectDescriptions));
            generation.markCompleted(summaryResponse.content().trim(), SummaryGenerationPromptBuilder.VERSION,
                    summaryResponse.model());
        } catch (LlmGenerationException | EmbeddingGenerationException e) {
            // Both are provider/config failures (no API key, provider error, bad
            // response) discovered mid-pipeline - retrieval and similarity scoring
            // call embed() too, not just the knowledge-fragments endpoint, so an
            // EmbeddingGenerationException is just as reachable here as an LLM one.
            // Recorded on the row, not thrown as a 500 - see the class javadoc.
            generation.markFailed(e.getMessage());
        }

        return resumeGenerationRepository.save(generation);
    }

    @Transactional(readOnly = true)
    public ResumeGeneration getById(UUID id) {
        return resumeGenerationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resume generation not found: " + id));
    }

    private GeneratedProject generateProject(String primaryRole, ProjectGenerationInput input,
                                              List<String> siblingDescriptions) {
        ChronologyValidator.requireStartNotAfterEnd(input.startDate(), input.endDate());

        int startYear = input.startDate().getYear();
        int endYear = input.endDate().getYear();
        String role = input.role() != null && !input.role().isBlank() ? input.role() : primaryRole;

        List<String> approvedTechnologies = resolveApprovedTechnologies(input, startYear, endYear);
        List<String> referenceSnippets = retrieveReferenceSnippets(role, input, startYear, endYear);

        List<String> comparisonTexts = new ArrayList<>(referenceSnippets);
        comparisonTexts.addAll(siblingDescriptions);

        ProjectGenerationContext context = new ProjectGenerationContext(
                role, input.client(), input.startDate(), input.endDate(), input.domain(),
                approvedTechnologies, referenceSnippets);
        PromptMessages prompt = ProjectGenerationPromptBuilder.build(context);

        ProjectGenerationResult result;
        SimilarityCheckResult similarityResult;
        int attempt = 0;
        do {
            attempt++;
            String userPrompt = attempt == 1 ? prompt.user() : prompt.user() + REWRITE_HINT;
            LlmResponse response = llmClient.generate(new LlmRequest(prompt.system(), userPrompt));
            result = parseProjectResult(response.content());
            similarityResult = similarityValidator.evaluate(result.description(), comparisonTexts);
        } while (similarityResult.verdict() == SimilarityVerdict.REWRITE && attempt < MAX_REWRITE_ATTEMPTS);

        GeneratedProject project = new GeneratedProject(input.client(), role, input.startDate(), input.endDate(),
                input.domain());
        project.applyGenerated(result.description(), result.responsibilities(), result.environment(),
                ProjectGenerationPromptBuilder.VERSION, similarityResult, attempt);
        return project;
    }

    /**
     * §14: if the caller supplied known technologies, keep only the ones the
     * timeline engine doesn't flag as FAIL for this project's dates (PASS or
     * QUESTIONABLE both pass through - QUESTIONABLE is "flag for review, don't
     * block" per TimelineStatus's own contract). Otherwise fall back to the
     * era profile's own suggestions (§40) for these dates.
     */
    private List<String> resolveApprovedTechnologies(ProjectGenerationInput input, int startYear, int endYear) {
        if (input.knownTechnologies() != null && !input.knownTechnologies().isEmpty()) {
            return technologyTimelineValidator.checkAll(input.knownTechnologies(), startYear, endYear).stream()
                    .filter(check -> check.status() == TimelineStatus.PASS || check.status() == TimelineStatus.QUESTIONABLE)
                    .map(check -> check.technologyName())
                    .toList();
        }
        return technologyTimelineValidator.suggestAlternatives(startYear, endYear);
    }

    private List<String> retrieveReferenceSnippets(String role, ProjectGenerationInput input,
                                                     int startYear, int endYear) {
        String queryText = buildRetrievalQuery(role, input);
        RetrievalFilter filter = new RetrievalFilter(input.domain(), role, null, startYear, endYear);
        return retrievalService.retrieveSimilar(queryText, filter, REFERENCE_SNIPPET_LIMIT).stream()
                .map(KnowledgeFragment::getContent)
                .toList();
    }

    private String buildRetrievalQuery(String role, ProjectGenerationInput input) {
        StringBuilder query = new StringBuilder(role).append(" project for ").append(input.client());
        if (input.domain() != null && !input.domain().isBlank()) {
            query.append(" in the ").append(input.domain()).append(" domain");
        }
        query.append(" during ").append(input.startDate().getYear()).append('-').append(input.endDate().getYear());
        return query.toString();
    }

    private LlmRequest buildSummaryPrompt(ResumeGenerationRequest request, List<String> projectDescriptions) {
        SummaryGenerationContext context = new SummaryGenerationContext(
                request.candidateName(), request.primaryRole(), request.totalExperienceYears(), projectDescriptions);
        PromptMessages prompt = SummaryGenerationPromptBuilder.build(context);
        return new LlmRequest(prompt.system(), prompt.user());
    }

    private ProjectGenerationResult parseProjectResult(String content) {
        JsonNode root;
        try {
            root = objectMapper.readTree(content);
        } catch (IOException e) {
            throw new LlmGenerationException("Could not parse LLM project-generation response: " + content, e);
        }
        String description = root.path("description").asText("");
        List<String> responsibilities = toStringList(root.path("responsibilities"));
        List<String> environment = toStringList(root.path("environment"));
        return new ProjectGenerationResult(description, responsibilities, environment);
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

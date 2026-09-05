package com.company.resumeai.ingestion;

import com.company.resumeai.common.exception.InvalidRequestException;
import com.company.resumeai.common.exception.ResourceNotFoundException;
import com.company.resumeai.embedding.EmbeddingGenerationException;
import com.company.resumeai.knowledge.FragmentType;
import com.company.resumeai.knowledge.KnowledgeFragmentCreateRequest;
import com.company.resumeai.knowledge.KnowledgeFragmentService;
import com.company.resumeai.llm.LlmGenerationException;
import com.company.resumeai.parser.ParsedProject;
import com.company.resumeai.parser.ParsedResume;
import com.company.resumeai.parser.ResumeParser;
import com.company.resumeai.parser.ResumeParsingException;
import com.company.resumeai.parser.ResumeTextExtractor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * §9 resume ingestion flow orchestrator: extract text, parse via LLM, persist
 * the resume_source row either way, then create knowledge fragments (+
 * embeddings) from whatever the parser found.
 *
 * Deliberately NOT @Transactional, same reasoning as
 * generation.ResumeGenerationService.generate() (see its javadoc):
 * resumeParser.parse() and knowledgeFragmentService.create() each call out to
 * the LLM/embedding APIs from their own separately-transactional beans/calls;
 * wrapping this method in one transaction risks the identical
 * UnexpectedRollbackException bug found and fixed in Milestone 5/6. The
 * initial save() (so raw text is preserved even if parsing later fails) and
 * the final save() each get their own transaction via Spring Data's
 * repository proxy - nothing here needs a wider one.
 */
@Service
public class ResumeUploadService {

    private static final Pattern YEAR_PATTERN = Pattern.compile("\\d{4}");

    private final ResumeSourceRepository resumeSourceRepository;
    private final ResumeTextExtractor resumeTextExtractor;
    private final ResumeParser resumeParser;
    private final KnowledgeFragmentService knowledgeFragmentService;
    private final ObjectMapper objectMapper;

    public ResumeUploadService(ResumeSourceRepository resumeSourceRepository,
                                ResumeTextExtractor resumeTextExtractor,
                                ResumeParser resumeParser,
                                KnowledgeFragmentService knowledgeFragmentService,
                                ObjectMapper objectMapper) {
        this.resumeSourceRepository = resumeSourceRepository;
        this.resumeTextExtractor = resumeTextExtractor;
        this.resumeParser = resumeParser;
        this.knowledgeFragmentService = knowledgeFragmentService;
        this.objectMapper = objectMapper;
    }

    public ResumeSource upload(MultipartFile file, UUID candidateId) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("No file provided");
        }
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new InvalidRequestException("Could not read uploaded file: " + e.getMessage());
        }

        // Throws InvalidRequestException (400) on an unsupported/corrupt file - deliberately
        // not caught here, so a bad upload never even creates a resume_source row.
        String rawText = resumeTextExtractor.extract(fileName, content);

        ResumeSource resumeSource = resumeSourceRepository.save(
                new ResumeSource(candidateId, fileName, extensionOf(fileName), rawText));

        try {
            ParsedResume parsedResume = resumeParser.parse(rawText);
            resumeSource.markParsed(objectMapper.writeValueAsString(parsedResume));
            createKnowledgeFragments(resumeSource.getId(), candidateId, parsedResume);
        } catch (LlmGenerationException | ResumeParsingException | EmbeddingGenerationException e) {
            resumeSource.markFailed(e.getMessage());
        } catch (JsonProcessingException e) {
            resumeSource.markFailed("Could not serialize parsed resume: " + e.getMessage());
        }

        return resumeSourceRepository.save(resumeSource);
    }

    @Transactional(readOnly = true)
    public ResumeSource getById(UUID id) {
        return resumeSourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resume source not found: " + id));
    }

    /**
     * Deletes a resume_source and every knowledge fragment created from it.
     * Unlike upload(), this is plain @Transactional - no LLM/embedding calls
     * happen here, just two DB deletes, so none of the nested-transaction
     * rollback risk that keeps upload() from being @Transactional applies.
     * The FK itself is ON DELETE SET NULL (V4 migration), not CASCADE, so
     * without this explicit fragment delete first, re-running the same bad
     * upload-then-delete cycle would leave orphaned duplicate fragments
     * behind indefinitely instead of actually removing them.
     */
    @Transactional
    public void delete(UUID id) {
        if (!resumeSourceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Resume source not found: " + id);
        }
        knowledgeFragmentService.deleteBySourceResumeId(id);
        resumeSourceRepository.deleteById(id);
    }

    /**
     * §9 "Create Knowledge Fragments" + "Generate Embeddings", per project:
     * one PROJECT_SUMMARY fragment from the responsibilities, one TECH_STACK
     * fragment from the technology list - matches §24's recommended chunking
     * (separate fragments per content type, not one fragment for the whole
     * resume). candidate_project rows are deliberately NOT created here - §31
     * Screen 1's "Edit parsed projects" / "Confirm technologies" imply that's
     * a separate, human-gated confirmation step, not automatic on upload.
     *
     * §20's non-project sections (name/contact, education, certifications) are
     * parsed (see ParsedCandidate) and returned in the response, but deliberately
     * don't become fragments here - they're identity/qualification metadata, not
     * reusable semantic content in §8.7's sense (nothing would ever retrieve "has
     * a B.S. in Computer Science" as a relevant project pattern). The candidate's
     * overall technicalSkills list is the one exception: unlike per-project
     * technologies (already captured above, each scoped to its own date range),
     * this is a genuine standalone skill signal not tied to any single project,
     * so it gets its own candidate-level TECH_STACK fragment (no date range -
     * it's not scoped to one project's timeline). The free-text "summary" field
     * does NOT get a fragment - it's candidate narrative, not retrievable project
     * content, and generation.SummaryGenerationPromptBuilder already synthesizes
     * a fresh one per generation rather than reusing an existing one verbatim.
     */
    private void createKnowledgeFragments(UUID resumeSourceId, UUID candidateId, ParsedResume parsedResume) {
        String primaryRole = parsedResume.candidate() != null ? parsedResume.candidate().primaryRole() : null;

        for (ParsedProject project : parsedResume.projects()) {
            Short startYear = yearOf(project.startDate());
            Short endYear = yearOf(project.endDate());
            String role = project.role() != null ? project.role() : primaryRole;

            if (!project.responsibilities().isEmpty()) {
                knowledgeFragmentService.create(new KnowledgeFragmentCreateRequest(
                        candidateId, null, null, FragmentType.PROJECT_SUMMARY,
                        String.join(". ", project.responsibilities()),
                        project.domain(), role, startYear, endYear, resumeSourceId));
            }
            if (!project.technologies().isEmpty()) {
                knowledgeFragmentService.create(new KnowledgeFragmentCreateRequest(
                        candidateId, null, null, FragmentType.TECH_STACK,
                        "Technologies used: " + String.join(", ", project.technologies()),
                        project.domain(), role, startYear, endYear, resumeSourceId));
            }
        }

        List<String> overallSkills = parsedResume.candidate() != null
                ? parsedResume.candidate().technicalSkills() : List.of();
        if (overallSkills != null && !overallSkills.isEmpty()) {
            knowledgeFragmentService.create(new KnowledgeFragmentCreateRequest(
                    candidateId, null, null, FragmentType.TECH_STACK,
                    "Overall technical skills: " + String.join(", ", overallSkills),
                    null, primaryRole, null, null, resumeSourceId));
        }
    }

    /** Resume dates come as free-form text ("2015-01", "2015", "Present") - just take the first 4-digit year found. */
    private Short yearOf(String dateText) {
        if (dateText == null) {
            return null;
        }
        Matcher matcher = YEAR_PATTERN.matcher(dateText);
        return matcher.find() ? Short.parseShort(matcher.group()) : null;
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 && dot < fileName.length() - 1 ? fileName.substring(dot + 1).toLowerCase(Locale.ROOT) : "unknown";
    }
}

package com.company.resumeai.knowledge;

import com.company.resumeai.retrieval.RetrievalFilter;
import com.company.resumeai.retrieval.RetrievalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Not in §21's original API list - added so Milestone 3 (embeddings +
 * retrieval) has a way to populate and query knowledge_fragment rows without
 * resume ingestion (Milestone 2) existing yet, same as §44's own
 * "manual data entry before automation" build order. See
 * IMPLEMENTATION_NOTES.md.
 */
@RestController
@RequestMapping("/api/v1/knowledge-fragments")
public class KnowledgeFragmentController {

    private final KnowledgeFragmentService knowledgeFragmentService;
    private final RetrievalService retrievalService;

    public KnowledgeFragmentController(KnowledgeFragmentService knowledgeFragmentService,
                                        RetrievalService retrievalService) {
        this.knowledgeFragmentService = knowledgeFragmentService;
        this.retrievalService = retrievalService;
    }

    @PostMapping
    public ResponseEntity<KnowledgeFragmentResponse> create(@Valid @RequestBody KnowledgeFragmentCreateRequest request) {
        KnowledgeFragment fragment = knowledgeFragmentService.create(request);
        KnowledgeFragmentResponse body = KnowledgeFragmentResponse.from(fragment);
        return ResponseEntity.created(URI.create("/api/v1/knowledge-fragments/" + fragment.getId())).body(body);
    }

    @GetMapping("/search")
    public List<KnowledgeFragmentResponse> search(@RequestParam String query,
                                                    @RequestParam(required = false) String domain,
                                                    @RequestParam(required = false) String role,
                                                    @RequestParam(required = false) UUID clientId,
                                                    @RequestParam(required = false) Integer startYear,
                                                    @RequestParam(required = false) Integer endYear,
                                                    @RequestParam(defaultValue = "10") int limit) {
        RetrievalFilter filter = new RetrievalFilter(domain, role, clientId, startYear, endYear);
        return retrievalService.retrieveSimilar(query, filter, limit).stream()
                .map(KnowledgeFragmentResponse::from)
                .toList();
    }
}

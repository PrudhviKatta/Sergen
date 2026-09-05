package com.company.resumeai.generation;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

/** §21 "Generate Base Resume" / "Get Generation". Regenerate/approve/export are later milestones (6/8). */
@RestController
@RequestMapping("/api/v1/resume-generations")
public class ResumeGenerationController {

    private final ResumeGenerationService resumeGenerationService;

    public ResumeGenerationController(ResumeGenerationService resumeGenerationService) {
        this.resumeGenerationService = resumeGenerationService;
    }

    @PostMapping
    public ResponseEntity<ResumeGenerationResponse> create(@Valid @RequestBody ResumeGenerationRequest request) {
        ResumeGeneration generation = resumeGenerationService.generate(request);
        ResumeGenerationResponse body = ResumeGenerationResponse.from(generation);
        return ResponseEntity.created(URI.create("/api/v1/resume-generations/" + generation.getId())).body(body);
    }

    @GetMapping("/{generationId}")
    public ResumeGenerationResponse get(@PathVariable UUID generationId) {
        return ResumeGenerationResponse.from(resumeGenerationService.getById(generationId));
    }
}

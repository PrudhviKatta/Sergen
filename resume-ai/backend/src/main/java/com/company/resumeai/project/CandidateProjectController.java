package com.company.resumeai.project;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/candidates/{candidateId}/projects")
public class CandidateProjectController {

    private final CandidateProjectService candidateProjectService;

    public CandidateProjectController(CandidateProjectService candidateProjectService) {
        this.candidateProjectService = candidateProjectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@PathVariable UUID candidateId,
                                                    @Valid @RequestBody ProjectCreateRequest request) {
        CandidateProject project = candidateProjectService.create(candidateId, request);
        ProjectResponse body = ProjectResponse.from(project);
        return ResponseEntity
                .created(URI.create("/api/v1/candidates/" + candidateId + "/projects/" + project.getId()))
                .body(body);
    }

    @GetMapping
    public List<ProjectResponse> listByCandidate(@PathVariable UUID candidateId) {
        return candidateProjectService.listByCandidate(candidateId).stream()
                .map(ProjectResponse::from)
                .toList();
    }
}

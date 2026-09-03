package com.company.resumeai.candidate;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/candidates")
public class CandidateController {

    private final CandidateService candidateService;

    public CandidateController(CandidateService candidateService) {
        this.candidateService = candidateService;
    }

    @PostMapping
    public ResponseEntity<CandidateResponse> create(@Valid @RequestBody CandidateCreateRequest request) {
        Candidate candidate = candidateService.create(request);
        CandidateResponse body = CandidateResponse.from(candidate);
        return ResponseEntity.created(URI.create("/api/v1/candidates/" + candidate.getId())).body(body);
    }

    @GetMapping("/{candidateId}")
    public CandidateResponse getById(@PathVariable UUID candidateId) {
        return CandidateResponse.from(candidateService.getById(candidateId));
    }
}

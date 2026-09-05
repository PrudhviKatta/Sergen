package com.company.resumeai.ingestion;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.UUID;

/**
 * §21 "Upload Resume" / "Get Parsed Resume". Returns the full
 * ResumeSourceResponse from POST (not just §21's minimal example
 * {resumeId, status}) - consistent with every other create endpoint in this
 * app (POST /clients, /candidates, /resume-generations all return the full
 * created resource), and since processing is synchronous (no queue/async
 * infra exists anywhere in this app yet), the final status is already known
 * by the time the response is written - returning a placeholder "PROCESSING"
 * that will never actually change would be dishonest.
 */
@RestController
@RequestMapping("/api/v1/resumes")
public class ResumeUploadController {

    private final ResumeUploadService resumeUploadService;

    public ResumeUploadController(ResumeUploadService resumeUploadService) {
        this.resumeUploadService = resumeUploadService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumeSourceResponse> upload(@RequestParam("file") MultipartFile file,
                                                         @RequestParam(required = false) UUID candidateId) {
        ResumeSource resumeSource = resumeUploadService.upload(file, candidateId);
        ResumeSourceResponse body = ResumeSourceResponse.from(resumeSource);
        return ResponseEntity.created(URI.create("/api/v1/resumes/" + resumeSource.getId())).body(body);
    }

    @GetMapping("/{resumeId}")
    public ResumeSourceResponse get(@PathVariable UUID resumeId) {
        return ResumeSourceResponse.from(resumeUploadService.getById(resumeId));
    }

    // Not in §21's original API list - added so a duplicate/bad upload can
    // actually be removed (including its knowledge fragments - see
    // ResumeUploadService.delete()) instead of only being fixable with raw SQL.
    @DeleteMapping("/{resumeId}")
    public ResponseEntity<Void> delete(@PathVariable UUID resumeId) {
        resumeUploadService.delete(resumeId);
        return ResponseEntity.noContent().build();
    }
}

package com.company.resumeai.candidate;

import com.company.resumeai.common.exception.InvalidRequestException;
import com.company.resumeai.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CandidateService {

    private final CandidateRepository candidateRepository;

    public CandidateService(CandidateRepository candidateRepository) {
        this.candidateRepository = candidateRepository;
    }

    @Transactional
    public Candidate create(CandidateCreateRequest request) {
        candidateRepository.findByEmail(request.email()).ifPresent(existing -> {
            throw new InvalidRequestException("A candidate with email " + request.email() + " already exists");
        });

        Candidate candidate = new Candidate(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.primaryRole(),
                request.totalExperienceYears(),
                request.summary()
        );
        return candidateRepository.save(candidate);
    }

    @Transactional(readOnly = true)
    public Candidate getById(UUID candidateId) {
        return candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + candidateId));
    }
}

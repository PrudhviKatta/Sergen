package com.company.resumeai.generation;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ResumeGenerationRepository extends JpaRepository<ResumeGeneration, UUID> {

    // ResumeGenerationResponse.from() reads getProjects() after the request handler
    // returns from the service's @Transactional method - same open-in-view: false
    // trap as CandidateProjectRepository (Milestone 1). Fetch it eagerly here.
    @Override
    @EntityGraph(attributePaths = "projects")
    Optional<ResumeGeneration> findById(UUID id);
}

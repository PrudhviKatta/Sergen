package com.company.resumeai.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CandidateProjectRepository extends JpaRepository<CandidateProject, UUID> {

    List<CandidateProject> findByCandidateIdOrderByStartDateAsc(UUID candidateId);
}

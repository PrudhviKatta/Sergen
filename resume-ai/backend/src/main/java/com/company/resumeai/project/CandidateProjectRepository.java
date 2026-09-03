package com.company.resumeai.project;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CandidateProjectRepository extends JpaRepository<CandidateProject, UUID> {

    // ProjectResponse.from() reads client.getName() and candidate.getId(), both
    // lazy @ManyToOne associations. Without eagerly fetching them here, that read
    // happens in the controller after the transaction (and Hibernate session,
    // open-in-view is off) has already closed, throwing LazyInitializationException.
    @EntityGraph(attributePaths = {"candidate", "client"})
    List<CandidateProject> findByCandidateIdOrderByStartDateAsc(UUID candidateId);
}

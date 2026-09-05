package com.company.resumeai.ingestion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ResumeSourceRepository extends JpaRepository<ResumeSource, UUID> {
}

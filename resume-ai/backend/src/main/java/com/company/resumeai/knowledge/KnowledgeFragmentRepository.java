package com.company.resumeai.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface KnowledgeFragmentRepository extends JpaRepository<KnowledgeFragment, UUID> {
}

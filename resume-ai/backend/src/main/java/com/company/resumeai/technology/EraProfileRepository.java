package com.company.resumeai.technology;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EraProfileRepository extends JpaRepository<EraProfile, UUID> {

    @EntityGraph(attributePaths = "technologies")
    List<EraProfile> findAll();
}

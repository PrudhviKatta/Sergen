package com.company.resumeai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared base for controller-level integration tests. Spins up a real
 * Postgres+pgvector container (Flyway runs the real V1 migration against it)
 * so these exercise the actual schema, not an in-memory substitute.
 *
 * The container is started manually in a static initializer and NEVER
 * stopped by us - this is Testcontainers' documented "singleton container"
 * pattern for sharing one container across multiple test classes via a
 * shared base class. Deliberately NOT using @Testcontainers/@Container here:
 * that annotation pair manages start/stop per test class, and since this
 * static field is inherited (one field, shared identity across every
 * subclass), the first class to finish would stop the container out from
 * under every class that runs after it - which is exactly what happened the
 * first time this was run (ClientApiIT and CandidateProjectApiIT both got
 * "Connection refused" once CandidateApiIT's class-level teardown killed the
 * container - see git history / IMPLEMENTATION_NOTES.md). The Ryuk reaper
 * Testcontainers starts alongside the container cleans it up when the JVM
 * exits, so nothing leaks even without an explicit stop() here.
 *
 * Requires a local Docker daemon.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("resumeai")
            .withUsername("resumeai")
            .withPassword("resumeai");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected MockMvc mockMvc;
}

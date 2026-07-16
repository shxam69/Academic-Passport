package com.academicpassport.config;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for any test that needs the real application context wired against
 * a real PostgreSQL instance — required here because the schema leans on
 * Postgres-specific features (native enums, JSONB, partial unique indexes) that
 * H2's compatibility mode does not faithfully emulate. A green test against H2
 * would not actually prove these migrations work.
 * <p>
 * Requires Docker to be available wherever this test suite runs. It was NOT run
 * in the sandbox that generated this code (confirmed no Docker present as of
 * Module 2) — run {@code mvn test} locally or in CI to actually execute this.
 */
@Tag("integration")
@Testcontainers
@SpringBootTest
public abstract class AbstractIntegrationTest {

    // Pinned to postgres:17-alpine to match docker-compose.yml exactly — a test
    // passing against a different major version than production runs on would be
    // worse than no test at all (false confidence).
    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"))
                    .withDatabaseName("academic_passport_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}

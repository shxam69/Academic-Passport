package com.academicpassport;

import com.academicpassport.config.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * As of Module 3, this is no longer a trivial "does the app boot" check — it now
 * proves the full stack wires up correctly: all 7 Flyway migrations apply cleanly
 * against a real Postgres 17 instance, and every JPA entity's mappings validate
 * against the resulting schema (ddl-auto=validate would fail this test loudly if
 * an entity's annotations don't match what the migrations actually created).
 * That mapping-validation step is precisely why this test matters more here than
 * it did in Module 1 — it's the single check that would catch an entity/migration
 * drift before it reaches a real deployment.
 */
class AcademicPassportApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoadsAndAllMigrationsApplyAndEntitiesValidateAgainstSchema() {
        // Intentionally empty: a failed context load (migration error, ddl-auto
        // validation mismatch, misconfigured datasource) fails this test on its own.
    }
}

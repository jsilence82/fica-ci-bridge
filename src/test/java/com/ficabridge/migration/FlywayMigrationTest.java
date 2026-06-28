package com.ficabridge.migration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Verifies that all Flyway migrations run cleanly against H2 in PostgreSQL
 * compatibility mode, and that Hibernate schema validation passes against the
 * migrated schema.  If the Spring context loads without error, both checks passed.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.datasource.url=jdbc:h2:mem:flywaytest;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL"
})
class FlywayMigrationTest {

    @Test
    void migrationsApplyCleanlyAndSchemaMatchesEntities() {
        // Context load is the assertion — Flyway applies V1→V3 then Hibernate
        // validates every entity column against the migrated schema.
    }
}

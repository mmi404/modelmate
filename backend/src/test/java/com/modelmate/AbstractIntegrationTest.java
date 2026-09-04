package com.modelmate;

import com.modelmate.auth.TestPasswordResetCodeCapture;
import com.modelmate.security.RateLimitingFilter;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests. Boots the full Spring context against a
 * single, JVM-wide PostgreSQL container (singleton pattern) with Flyway
 * migrations applied. Tests commit for real; mutable tables are reset to the
 * seeded baseline before each test so cases stay independent.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestPasswordResetCodeCapture.class)
public abstract class AbstractIntegrationTest {

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("modelmate")
            .withUsername("modelmate")
            .withPassword("modelmate");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void resetToSeedBaseline() {
        jdbc.execute("truncate table votes, password_reset_tokens, replies, discussion_tags, "
                + "discussions, reviews restart identity cascade");
        jdbc.update("delete from models where submitted_by <> "
                + "(select id from users where email = 'system@modelmate.local')");
        jdbc.update("delete from users where email not in "
                + "('system@modelmate.local', 'admin@modelmate.local')");
        TestPasswordResetCodeCapture.LAST_CODE.set(null);
        rateLimitingFilter.clearBuckets();
    }
}

package com.goldenowl.gscores.support;

import com.goldenowl.gscores.repository.KetQuaThiRepository;
import com.goldenowl.gscores.repository.MigrationCheckpointRepository;
import com.goldenowl.gscores.repository.MonThiRepository;
import com.goldenowl.gscores.repository.NgoaiNguRepository;
import com.goldenowl.gscores.repository.ThiSinhRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for controller-level API tests: boots the real Spring context against
 * Testcontainers Postgres + Redis (no mocks), so requests exercise the actual
 * MockMvc -> controller -> service -> repository -> DB/cache path, including the
 * Redis-vs-Postgres fallback branches in ReportService/LeaderboardService. Flyway
 * runs on the container on startup, so mon_thi/ngoai_ngu reference data (V2 seed)
 * is always present; per-test data (thi_sinh/ket_qua_thi/migration_checkpoint) is
 * wiped before each test to keep tests independent.
 *
 * Containers are a JVM-wide singleton started once in a static initializer (not
 * JUnit-managed via @Container/@Testcontainers) because static fields declared on
 * this abstract class are a single shared slot across every subclass - letting
 * @Testcontainers stop/restart them per test class reassigns their mapped ports
 * out from under an already-built HikariCP pool, which manifests as intermittent
 * "connection refused" failures once a second test class runs.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class AbstractApiIT {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ThiSinhRepository thiSinhRepository;

    @Autowired
    protected KetQuaThiRepository ketQuaThiRepository;

    @Autowired
    protected MonThiRepository monThiRepository;

    @Autowired
    protected NgoaiNguRepository ngoaiNguRepository;

    @Autowired
    protected MigrationCheckpointRepository checkpointRepository;

    @BeforeEach
    void cleanPerTestData() {
        ketQuaThiRepository.deleteAll();
        thiSinhRepository.deleteAll();
        checkpointRepository.deleteAll();
    }
}

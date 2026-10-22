package com.hrstack.hr_stack.repository;

import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.entity.LoginChallenge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)   // <-- KEY: use our Postgres test DB, not H2
@ActiveProfiles("test")
@DisplayName("LoginChallengeRepository atomic queries")
class LoginChallengeRepositoryTest {

    @Autowired
    private LoginChallengeRepository challengeRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private TestEntityManager entityManager;

    private UUID challengeId;

    @BeforeEach
    void seed() {
        long now = System.currentTimeMillis();

        Employee employee = new Employee();
        // employee.setId(...) intentionally NOT set -> @GeneratedValue generates it
        employee.setEmpId("HRStack_2025_IT_REPO_001");
        employee.setFirstName("Repo");
        employee.setLastName("Test");
        employee.setEmail("repo-test@example.com");
        employee.setMobile("1234567890");
        employee.setPassword("Test@1234");
        employee.setRole("EMPLOYEE");
        employee.setStatus("ACTIVE");
        employee.setCreatedOn(now);
        employee.setUpdatedOn(now);

        Employee savedEmployee = entityManager.persistAndFlush(employee);

        LoginChallenge c = new LoginChallenge();
        c.setEmployeeId(savedEmployee.getId());
        c.setOtpHash("stored-hash");
        c.setCreatedOn(now);
        c.setLastSentOn(now);
        c.setExpiresOn(now + 5 * 60_000L);
        c.setAttempts(0);
        c.setResendCount(0);
        c.setUsed(false);

        LoginChallenge saved = entityManager.persistAndFlush(c);
        challengeId = saved.getId();

        entityManager.clear();
    }

    @Test
    @DisplayName("reserveAttempt increments attempts while below max and returns 1")
    void reserveAttempt_happyPath() {
        int updated = challengeRepository.reserveAttempt(challengeId, 5);
        assertEquals(1, updated);

        LoginChallenge reloaded = challengeRepository.findById(challengeId).orElseThrow();
        assertEquals(1, reloaded.getAttempts());
    }

    @Test
    @DisplayName("reserveAttempt returns 0 once attempts == max (does NOT increment past max)")
    void reserveAttempt_maxReached() {
        for (int i = 0; i < 5; i++) {
            assertEquals(1, challengeRepository.reserveAttempt(challengeId, 5),
                    "attempt " + (i + 1) + " should succeed");
        }
        // 6th call: attempts already == 5, WHERE clause excludes it
        assertEquals(0, challengeRepository.reserveAttempt(challengeId, 5));

        LoginChallenge reloaded = challengeRepository.findById(challengeId).orElseThrow();
        assertEquals(5, reloaded.getAttempts(), "attempts must not exceed max");
    }

    @Test
    @DisplayName("reserveAttempt returns 0 when challenge is already used")
    void reserveAttempt_usedChallenge() {
        assertEquals(1, challengeRepository.markUsed(challengeId));

        int updated = challengeRepository.reserveAttempt(challengeId, 5);
        assertEquals(0, updated);

        LoginChallenge reloaded = challengeRepository.findById(challengeId).orElseThrow();
        assertEquals(0, reloaded.getAttempts(), "attempts must not change on a used row");
    }

    @Test
    @DisplayName("markUsed sets used=true once; second call returns 0 (double-spend guard)")
    void markUsed_doubleSpendGuard() {
        int first = challengeRepository.markUsed(challengeId);
        int second = challengeRepository.markUsed(challengeId);

        assertEquals(1, first);
        assertEquals(0, second, "second markUsed must return 0 to prevent double login");

        LoginChallenge reloaded = challengeRepository.findById(challengeId).orElseThrow();
        assertTrue(reloaded.isUsed());
    }

    @Test
    @DisplayName("markUsed on missing id returns 0 (no exception)")
    void markUsed_missing() {
        assertEquals(0, challengeRepository.markUsed(UUID.randomUUID()));
    }

    @Test
    @DisplayName("freshly seeded challenge is unused with 0 attempts")
    void seedSanity() {
        LoginChallenge reloaded = challengeRepository.findById(challengeId).orElseThrow();
        assertFalse(reloaded.isUsed());
        assertEquals(0, reloaded.getAttempts());
        assertTrue(reloaded.getEmployeeId() != null);
    }
}
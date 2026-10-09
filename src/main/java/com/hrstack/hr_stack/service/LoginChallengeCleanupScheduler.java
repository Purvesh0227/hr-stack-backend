package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.repository.LoginChallengeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class LoginChallengeCleanupScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(LoginChallengeCleanupScheduler.class);

    // same as CHALLENGE_MAX_AGE_MS in LoginOtpService
    private static final long MAX_AGE_MS = 15 * 60 * 1000L;

    private final LoginChallengeRepository repository;

    public LoginChallengeCleanupScheduler(LoginChallengeRepository repository) {
        this.repository = repository;
    }

    @Scheduled(fixedRate = 60 * 60 * 1000L)   // every hour
    public void deleteOldChallenges() {
        int deleted = repository.deleteOlderThan(
                System.currentTimeMillis() - MAX_AGE_MS);
        if (deleted > 0) {
            log.info("Deleted {} expired login challenges", deleted);
        }
    }
}
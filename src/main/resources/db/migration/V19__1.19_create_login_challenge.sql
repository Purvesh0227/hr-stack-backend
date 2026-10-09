CREATE TABLE login_challenge (
                                 id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                 employee_id   UUID         NOT NULL,
                                 otp_hash      VARCHAR(100) NOT NULL,
                                 created_on    BIGINT       NOT NULL,
                                 last_sent_on  BIGINT       NOT NULL,
                                 expires_on    BIGINT       NOT NULL,
                                 attempts      INT          NOT NULL DEFAULT 0,
                                 resend_count  INT          NOT NULL DEFAULT 0,
                                 used          BOOLEAN      NOT NULL DEFAULT FALSE,
                                 CONSTRAINT fk_login_challenge_employee
                                     FOREIGN KEY (employee_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_login_challenge_employee_created
    ON login_challenge(employee_id, created_on DESC);

CREATE INDEX idx_login_challenge_expires
    ON login_challenge(expires_on);
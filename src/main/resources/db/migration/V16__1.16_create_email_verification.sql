CREATE TABLE email_verification (
                                    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                    email       VARCHAR(255) NOT NULL,
                                    otp         VARCHAR(100) NOT NULL,
                                    created_on  BIGINT  NOT NULL,
                                    expires_on  BIGINT  NOT NULL,
                                    verified    BOOLEAN NOT NULL DEFAULT FALSE,
                                    attempts    INT     NOT NULL DEFAULT 0
);

CREATE INDEX idx_email_verification_email_created
    ON email_verification(email, created_on DESC);
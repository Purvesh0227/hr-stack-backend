ALTER TABLE email_verification
    ALTER COLUMN otp DROP NOT NULL;

ALTER TABLE email_verification
    ADD COLUMN token_hash VARCHAR(64);

CREATE UNIQUE INDEX idx_email_verification_token_hash
    ON email_verification(token_hash)
    WHERE token_hash IS NOT NULL;
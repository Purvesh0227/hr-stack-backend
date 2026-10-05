CREATE TABLE password_reset_otp (
                                    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                    employee_id UUID    NOT NULL,
                                    otp         VARCHAR(100) NOT NULL,
                                    created_on  BIGINT  NOT NULL,
                                    expires_on  BIGINT  NOT NULL,
                                    verified    BOOLEAN NOT NULL DEFAULT FALSE,
                                    attempts    INT     NOT NULL DEFAULT 0,
                                    CONSTRAINT fk_pwd_reset_employee
                                        FOREIGN KEY (employee_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_pwd_reset_employee_created
    ON password_reset_otp(employee_id, created_on DESC);
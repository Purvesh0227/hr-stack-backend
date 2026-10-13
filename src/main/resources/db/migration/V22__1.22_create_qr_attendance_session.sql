
CREATE TABLE qr_attendance_session (
                                       uuid        UUID         NOT NULL PRIMARY KEY,
                                       token_hash  VARCHAR(64)  NOT NULL,
                                       created_by  UUID         NOT NULL,
                                       department  VARCHAR(255) NOT NULL,
                                       created_on  BIGINT       NOT NULL,
                                       expires_on  BIGINT       NOT NULL,
                                       status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
);

CREATE UNIQUE INDEX uq_qr_session_token_hash ON qr_attendance_session (token_hash);
CREATE INDEX idx_qr_session_department_status ON qr_attendance_session (department, status);


ALTER TABLE attendance
    ADD COLUMN method          VARCHAR(10) NOT NULL DEFAULT 'OTP',
    ADD COLUMN session_id      UUID        NULL,
    ADD COLUMN attendance_date DATE        NULL;

CREATE UNIQUE INDEX uq_attendance_emp_day ON attendance (emp_id, attendance_date);
CREATE INDEX idx_attendance_session_id ON attendance (session_id);
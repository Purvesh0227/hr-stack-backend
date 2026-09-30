CREATE TABLE notification (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                              employee_id UUID NOT NULL,

                              email VARCHAR(255) NOT NULL,

                              mobile VARCHAR(20) NOT NULL,

                              event_type VARCHAR(50) NOT NULL,

                              message TEXT NOT NULL,

                              is_read BOOLEAN NOT NULL DEFAULT FALSE,

                              created_on BIGINT NOT NULL,

                              CONSTRAINT fk_notification_employee
                                  FOREIGN KEY (employee_id)
                                      REFERENCES users(id)
                                      ON DELETE CASCADE
);

CREATE INDEX idx_notification_employee_id
    ON notification(employee_id);

CREATE INDEX idx_notification_employee_read
    ON notification(employee_id, is_read);

CREATE INDEX idx_notification_created_on
    ON notification(created_on);
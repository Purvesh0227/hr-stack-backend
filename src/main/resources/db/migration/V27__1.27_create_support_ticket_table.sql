CREATE TABLE support_ticket (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                employee_id UUID NOT NULL,
                                employee_name VARCHAR(255) NOT NULL,
                                employee_email VARCHAR(255) NOT NULL,
                                employee_mobile VARCHAR(20) NOT NULL,
                                subject VARCHAR(255) NOT NULL,
                                message TEXT NOT NULL,
                                priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
                                status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
                                created_on BIGINT NOT NULL,
                                resolved_on BIGINT,

                                CONSTRAINT fk_support_ticket_employee
                                    FOREIGN KEY (employee_id)
                                        REFERENCES users(id)
                                        ON DELETE CASCADE
);

CREATE INDEX idx_support_ticket_status ON support_ticket (status);
CREATE INDEX idx_support_ticket_created_on ON support_ticket (created_on);
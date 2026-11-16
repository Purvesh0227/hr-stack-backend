-- 1. Create a table to track status changes
CREATE TABLE support_ticket_status_history (
                                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                               ticket_id UUID NOT NULL,
                                               status VARCHAR(50) NOT NULL,
                                               changed_on BIGINT NOT NULL,

                                               CONSTRAINT fk_status_history_ticket
                                                   FOREIGN KEY (ticket_id) REFERENCES support_ticket(id) ON DELETE CASCADE
);

CREATE INDEX idx_status_history_ticket_id ON support_ticket_status_history(ticket_id);
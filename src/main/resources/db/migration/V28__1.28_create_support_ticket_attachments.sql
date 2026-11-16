CREATE TABLE support_ticket_attachment (
                                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                           ticket_id UUID NOT NULL,
                                           file_name VARCHAR(255) NOT NULL,
                                           content_type VARCHAR(100) NOT NULL,
                                           file_size BIGINT NOT NULL,
                                           object_key VARCHAR(500) NOT NULL,

                                           CONSTRAINT fk_support_attachment_ticket
                                               FOREIGN KEY (ticket_id) REFERENCES support_ticket(id) ON DELETE CASCADE
);

CREATE INDEX idx_support_attachment_ticket ON support_ticket_attachment(ticket_id);
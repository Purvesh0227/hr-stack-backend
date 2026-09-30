CREATE TABLE notification_subscription (
                                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                           employee_id UUID NOT NULL,

                                           endpoint TEXT NOT NULL,
                                           p256dh TEXT NOT NULL,
                                           auth TEXT NOT NULL,

                                           created_on BIGINT NOT NULL,
                                           updated_on BIGINT NOT NULL,

                                           CONSTRAINT fk_notification_subscription_employee
                                               FOREIGN KEY (employee_id)
                                                   REFERENCES users(id)
                                                   ON DELETE CASCADE,

                                           CONSTRAINT uk_notification_subscription_endpoint
                                               UNIQUE (endpoint)
);

CREATE INDEX idx_notification_subscription_employee_id
    ON notification_subscription(employee_id);

ALTER TABLE users
    ADD COLUMN google_sub VARCHAR(255),
    ADD COLUMN google_linked_on BIGINT;

CREATE UNIQUE INDEX uk_users_google_sub
    ON users (google_sub);
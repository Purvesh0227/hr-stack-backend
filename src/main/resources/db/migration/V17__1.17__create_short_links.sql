CREATE TABLE short_links (
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             short_code VARCHAR(20) NOT NULL UNIQUE,
                             target_url TEXT NOT NULL,
                             created_on BIGINT NOT NULL,
                             expires_on BIGINT NOT NULL,
                             active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_short_links_code
    ON short_links(short_code);

CREATE INDEX idx_short_links_expires_on
    ON short_links(expires_on);
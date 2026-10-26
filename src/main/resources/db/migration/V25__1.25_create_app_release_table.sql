CREATE TABLE app_release (
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             version VARCHAR(50) NOT NULL,
                             release_notes TEXT NOT NULL,
                             release_date DATE NOT NULL,
                             status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
                             download_url VARCHAR(500) NOT NULL,
                             created_on BIGINT NOT NULL,
                             updated_on BIGINT NOT NULL
);

CREATE INDEX idx_app_release_status ON app_release (status);
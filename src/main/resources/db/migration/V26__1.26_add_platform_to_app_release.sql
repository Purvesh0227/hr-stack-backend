ALTER TABLE app_release
    ADD COLUMN platform VARCHAR(20) NOT NULL DEFAULT 'ANDROID';

CREATE INDEX idx_app_release_platform ON app_release (platform);
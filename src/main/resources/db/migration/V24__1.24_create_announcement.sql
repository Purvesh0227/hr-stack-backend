CREATE TABLE announcement (
                              id              UUID         NOT NULL PRIMARY KEY,
                              title           VARCHAR(150) NOT NULL,
                              message         TEXT         NOT NULL,
                              type            VARCHAR(30)  NOT NULL,
                              created_by      UUID         NOT NULL,
                              created_by_name VARCHAR(255) NOT NULL,
                              created_on      BIGINT       NOT NULL,
                              updated_on      BIGINT       NULL
);

CREATE INDEX idx_announcement_created_on ON announcement (created_on DESC);
CREATE INDEX idx_announcement_type ON announcement (type);

CREATE TABLE announcement_attachment (
                                         id              UUID         NOT NULL PRIMARY KEY,
                                         announcement_id UUID         NOT NULL,
                                         object_key      VARCHAR(500) NOT NULL,
                                         file_name       VARCHAR(255) NOT NULL,
                                         content_type    VARCHAR(100) NOT NULL,
                                         file_size       BIGINT       NOT NULL,
                                         CONSTRAINT fk_announcement_attachment_announcement
                                             FOREIGN KEY (announcement_id) REFERENCES announcement (id) ON DELETE CASCADE
);

CREATE INDEX idx_announcement_attachment_announcement
    ON announcement_attachment (announcement_id);
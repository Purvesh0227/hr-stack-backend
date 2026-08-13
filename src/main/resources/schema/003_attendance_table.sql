CREATE TABLE attendance (
                            uuid UUID NOT NULL PRIMARY KEY,
                            emp_id VARCHAR(255) NOT NULL,
                            marked_on BIGINT NOT NULL,
                            status VARCHAR(255) NOT NULL
);


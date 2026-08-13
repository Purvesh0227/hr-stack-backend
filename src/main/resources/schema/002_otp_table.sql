CREATE TABLE otp (
                     uuid UUID NOT NULL PRIMARY KEY,
                     created_by UUID NOT NULL,
                     created_on BIGINT NOT NULL,
                     otp_date BIGINT NOT NULL,
                     department VARCHAR(255) NOT NULL,
                     expires_on BIGINT NOT NULL,
                     otp VARCHAR(6) NOT NULL
);
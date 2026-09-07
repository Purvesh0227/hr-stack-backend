ALTER TABLE salary_slip
    ALTER COLUMN generated_at DROP DEFAULT;

ALTER TABLE salary_slip
ALTER COLUMN generated_at TYPE BIGINT
USING (
    EXTRACT(EPOCH FROM generated_at) * 1000
)::BIGINT;
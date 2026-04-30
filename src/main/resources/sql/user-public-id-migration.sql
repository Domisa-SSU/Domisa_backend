ALTER TABLE users
    ADD COLUMN public_id VARCHAR(16) NULL;

UPDATE users
SET public_id = REPLACE(REPLACE(TO_BASE64(RANDOM_BYTES(12)), '+', 'A'), '/', 'b')
WHERE public_id IS NULL;

ALTER TABLE users
    MODIFY COLUMN public_id VARCHAR(16) NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT uk_users_public_id UNIQUE (public_id);

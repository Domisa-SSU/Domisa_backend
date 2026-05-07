ALTER TABLE users
	DROP COLUMN free_blur_count,
	DROP COLUMN free_blur_reset_at,
	DROP COLUMN free_like_reset_at;

CREATE TABLE user_before_shows (
	user_id BIGINT NOT NULL,
	target_user_id BIGINT
);

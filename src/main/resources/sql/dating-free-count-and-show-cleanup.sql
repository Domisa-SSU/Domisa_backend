ALTER TABLE users
	DROP COLUMN free_blur_count,
	DROP COLUMN free_blur_reset_at,
	DROP COLUMN free_like_reset_at;

DROP TABLE IF EXISTS user_before_shows;

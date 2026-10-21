-- Administrative reporting, Karma reputation and paid text limits.
ALTER TABLE tb_users ADD COLUMN IF NOT EXISTS karma_points BIGINT NOT NULL DEFAULT 0;
ALTER TABLE tb_users DROP CONSTRAINT IF EXISTS ck_users_karma_points;
ALTER TABLE tb_users ADD CONSTRAINT ck_users_karma_points CHECK (karma_points >= 0);

ALTER TABLE tb_profiles ADD COLUMN IF NOT EXISTS extra_bio_chars INT NOT NULL DEFAULT 0;
ALTER TABLE tb_profiles ADD COLUMN IF NOT EXISTS extra_post_chars INT NOT NULL DEFAULT 0;
ALTER TABLE tb_profiles DROP CONSTRAINT IF EXISTS ck_profiles_extra_bio_chars;
ALTER TABLE tb_profiles ADD CONSTRAINT ck_profiles_extra_bio_chars CHECK (extra_bio_chars >= 0);
ALTER TABLE tb_profiles DROP CONSTRAINT IF EXISTS ck_profiles_extra_post_chars;
ALTER TABLE tb_profiles ADD CONSTRAINT ck_profiles_extra_post_chars CHECK (extra_post_chars >= 0);

CREATE TABLE IF NOT EXISTS karma_events (
    event_id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    points BIGINT NOT NULL CHECK (points > 0),
    reason VARCHAR(280) NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_karma_event_user FOREIGN KEY (user_id) REFERENCES tb_users(user_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_karma_events_user_created ON karma_events (user_id, created_at DESC);

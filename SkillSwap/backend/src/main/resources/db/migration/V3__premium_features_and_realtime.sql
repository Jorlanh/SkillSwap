-- Premium quotas and paid feature state.
CREATE TABLE IF NOT EXISTS premium_ai_usage (
    usage_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    period_start DATE NOT NULL,
    free_ai_prompts_used INT NOT NULL DEFAULT 0 CHECK (free_ai_prompts_used >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_premium_ai_usage_user_period UNIQUE (user_id, period_start),
    CONSTRAINT fk_premium_ai_usage_user FOREIGN KEY (user_id) REFERENCES tb_users(user_id) ON DELETE CASCADE
);

ALTER TABLE tb_skill_swap_proposals ADD COLUMN IF NOT EXISTS boosted_until TIMESTAMPTZ;
ALTER TABLE tb_posts ADD COLUMN IF NOT EXISTS highlighted_until TIMESTAMPTZ;
ALTER TABLE tb_profiles ADD COLUMN IF NOT EXISTS extra_portfolio_slots INT NOT NULL DEFAULT 0;
ALTER TABLE tb_profiles DROP CONSTRAINT IF EXISTS ck_profiles_extra_portfolio_slots;
ALTER TABLE tb_profiles ADD CONSTRAINT ck_profiles_extra_portfolio_slots CHECK (extra_portfolio_slots >= 0);

CREATE TABLE IF NOT EXISTS lesson_extra_purchases (
    purchase_id UUID PRIMARY KEY,
    lesson_id BIGINT NOT NULL,
    payer_id UUID NOT NULL,
    teacher_id UUID NOT NULL,
    hours INT NOT NULL CHECK (hours BETWEEN 1 AND 10),
    amount BIGINT NOT NULL CHECK (amount > 0),
    status VARCHAR(16) NOT NULL DEFAULT 'PAID' CHECK (status IN ('PAID', 'REFUNDED')),
    idempotency_key VARCHAR(160) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lesson_extra_lesson FOREIGN KEY (lesson_id) REFERENCES tb_lessons(lesson_id) ON DELETE CASCADE,
    CONSTRAINT fk_lesson_extra_payer FOREIGN KEY (payer_id) REFERENCES tb_users(user_id),
    CONSTRAINT fk_lesson_extra_teacher FOREIGN KEY (teacher_id) REFERENCES tb_users(user_id)
);

ALTER TABLE swapcoin_ledger DROP CONSTRAINT IF EXISTS ck_swapcoin_ledger_type;
ALTER TABLE swapcoin_ledger ADD CONSTRAINT ck_swapcoin_ledger_type CHECK (entry_type IN (
    'PURCHASE', 'WELCOME_BONUS', 'TIP_SENT', 'TIP_RECEIVED', 'FEATURE_SPEND',
    'ACHIEVEMENT_REWARD', 'LESSON_PAYMENT', 'REFUND', 'ADJUSTMENT'));

CREATE INDEX IF NOT EXISTS idx_premium_ai_usage_user_period ON premium_ai_usage (user_id, period_start DESC);
CREATE INDEX IF NOT EXISTS idx_lesson_extra_purchases_lesson ON lesson_extra_purchases (lesson_id, created_at DESC);

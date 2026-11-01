-- Recursos gratuitos: preferências explícitas de acessibilidade e conclusão bilateral de aulas.
ALTER TABLE tb_lessons ADD COLUMN IF NOT EXISTS title VARCHAR(180) NOT NULL DEFAULT 'Aula SkillSwap';
ALTER TABLE tb_lessons ADD COLUMN IF NOT EXISTS duration_minutes INTEGER NOT NULL DEFAULT 60;
ALTER TABLE tb_lessons ADD COLUMN IF NOT EXISTS teacher_confirmed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tb_lessons ADD COLUMN IF NOT EXISTS student_confirmed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tb_lessons ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE IF NOT EXISTS ss_accessibility_preferences (
    user_id UUID PRIMARY KEY,
    contrast_mode VARCHAR(24) NOT NULL DEFAULT 'default'
        CHECK (contrast_mode IN ('default', 'high-contrast')),
    font_scale VARCHAR(16) NOT NULL DEFAULT 'medium'
        CHECK (font_scale IN ('small', 'medium', 'large')),
    preferred_channel VARCHAR(16) NOT NULL DEFAULT 'any'
        CHECK (preferred_channel IN ('text', 'audio', 'video', 'any')),
    captions_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    image_descriptions_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    simplified_language_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    libras_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    keyboard_navigation_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    visual_marker VARCHAR(16) NOT NULL DEFAULT 'standard'
        CHECK (visual_marker IN ('standard', 'focus', 'contrast')),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_accessibility_preferences_user
        FOREIGN KEY (user_id) REFERENCES tb_users(user_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_lessons_participants_time
    ON tb_lessons (teacher_id, student_id, scheduled_time);

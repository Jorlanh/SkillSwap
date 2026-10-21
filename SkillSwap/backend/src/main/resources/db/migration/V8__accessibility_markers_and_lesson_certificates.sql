-- Preferências visuais escolhidas pela própria pessoa e emissão de certificados.
-- Os marcadores são uma opção de apresentação; não representam diagnóstico,
-- deficiência ou qualquer classificação automática do usuário.

ALTER TABLE tb_users
    ADD COLUMN IF NOT EXISTS accessibility_visual_marker VARCHAR(32) NOT NULL DEFAULT 'NEUTRAL_CIRCLE';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_users_accessibility_visual_marker'
    ) THEN
        ALTER TABLE tb_users
            ADD CONSTRAINT ck_users_accessibility_visual_marker
            CHECK (accessibility_visual_marker IN ('BLUE_TRIANGLE', 'ORANGE_SQUARE', 'NEUTRAL_CIRCLE'));
    END IF;
END $$;

ALTER TABLE tb_lessons
    ADD COLUMN IF NOT EXISTS title VARCHAR(200),
    ADD COLUMN IF NOT EXISTS introduction TEXT,
    ADD COLUMN IF NOT EXISTS duration_minutes INTEGER NOT NULL DEFAULT 60,
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS certificate_code VARCHAR(80);

CREATE UNIQUE INDEX IF NOT EXISTS ux_lessons_certificate_code
    ON tb_lessons (certificate_code)
    WHERE certificate_code IS NOT NULL;

ALTER TABLE tb_lessons
    ADD CONSTRAINT ck_lessons_duration_minutes
    CHECK (duration_minutes BETWEEN 15 AND 480);

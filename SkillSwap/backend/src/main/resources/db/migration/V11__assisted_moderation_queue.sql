-- Moderação assistida: a IA faz triagem e contenção de conteúdo; decisões
-- sobre contas ficam registradas e são tomadas por administradores humanos.

ALTER TABLE tb_reports ADD COLUMN IF NOT EXISTS reported_content TEXT;
ALTER TABLE tb_reports ADD COLUMN IF NOT EXISTS source VARCHAR(32) NOT NULL DEFAULT 'USER_REPORT';
ALTER TABLE tb_reports ADD COLUMN IF NOT EXISTS ai_category VARCHAR(64);
ALTER TABLE tb_reports ADD COLUMN IF NOT EXISTS ai_confidence NUMERIC(5,4);
ALTER TABLE tb_reports ADD COLUMN IF NOT EXISTS ai_recommendation VARCHAR(32);
ALTER TABLE tb_reports ADD COLUMN IF NOT EXISTS contained_until TIMESTAMP WITH TIME ZONE;
ALTER TABLE tb_reports ADD COLUMN IF NOT EXISTS moderation_action VARCHAR(32);
ALTER TABLE tb_reports ADD COLUMN IF NOT EXISTS reviewed_by_user_id UUID;
ALTER TABLE tb_reports ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE tb_reports ADD COLUMN IF NOT EXISTS decision_reason VARCHAR(2000);
ALTER TABLE tb_reports ADD COLUMN IF NOT EXISTS decision_evidence TEXT;
ALTER TABLE tb_reports ADD COLUMN IF NOT EXISTS appeal_status VARCHAR(32) NOT NULL DEFAULT 'NOT_REQUESTED';
ALTER TABLE tb_reports ADD COLUMN IF NOT EXISTS appeal_reason VARCHAR(2000);
ALTER TABLE tb_reports ADD COLUMN IF NOT EXISTS appealed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE tb_reports ADD COLUMN IF NOT EXISTS automatic_temporary_ban_applied BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tb_reports ADD COLUMN IF NOT EXISTS automatic_action_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE tb_reports ADD COLUMN IF NOT EXISTS automatic_action_reason VARCHAR(1000);

-- Casos antigos processados pelo fluxo automático voltam para a fila se não
-- possuem decisão humana registrada. A migration não remove o banimento; o
-- administrador deverá revisar e liberar ou manter a medida pelo painel.
UPDATE tb_reports
SET status = 'PENDING'
WHERE status IN ('REVIEWED', 'ACTION_TAKEN')
  AND moderation_action IS NULL;

ALTER TABLE tb_reports DROP CONSTRAINT IF EXISTS fk_reports_reviewed_by;
ALTER TABLE tb_reports ADD CONSTRAINT fk_reports_reviewed_by
    FOREIGN KEY (reviewed_by_user_id) REFERENCES tb_users(user_id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_reports_moderation_queue
    ON tb_reports(status, created_at ASC);

CREATE TABLE IF NOT EXISTS ss_moderation_settings (
    id INTEGER PRIMARY KEY,
    automatic_temporary_ban_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    automatic_temporary_ban_hours INTEGER NOT NULL DEFAULT 24,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_moderation_hours CHECK (automatic_temporary_ban_hours BETWEEN 1 AND 168)
);

INSERT INTO ss_moderation_settings(id, automatic_temporary_ban_enabled, automatic_temporary_ban_hours)
VALUES (1, FALSE, 24)
ON CONFLICT (id) DO NOTHING;

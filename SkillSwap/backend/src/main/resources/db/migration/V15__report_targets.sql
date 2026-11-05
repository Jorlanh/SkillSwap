ALTER TABLE tb_reports ADD COLUMN IF NOT EXISTS target_type VARCHAR(32) NOT NULL DEFAULT 'USER';
ALTER TABLE tb_reports ADD COLUMN IF NOT EXISTS target_id VARCHAR(64);
ALTER TABLE tb_reports ADD COLUMN IF NOT EXISTS category VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_reports_target ON tb_reports(target_type, target_id);

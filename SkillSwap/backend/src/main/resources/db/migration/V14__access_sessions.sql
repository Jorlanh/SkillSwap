ALTER TABLE tb_access_logs ADD COLUMN IF NOT EXISTS device_info VARCHAR(255);
ALTER TABLE tb_access_logs ADD COLUMN IF NOT EXISTS token_hash VARCHAR(128);
ALTER TABLE tb_access_logs ADD COLUMN IF NOT EXISTS revoked BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_access_logs_token_hash ON tb_access_logs(token_hash);

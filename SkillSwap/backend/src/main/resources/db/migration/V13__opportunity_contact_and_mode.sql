ALTER TABLE tb_access_opportunities
    ADD COLUMN IF NOT EXISTS participation_mode VARCHAR(20) NOT NULL DEFAULT 'REMOTE';

ALTER TABLE tb_access_opportunities
    ADD COLUMN IF NOT EXISTS contact_url VARCHAR(1000);

UPDATE tb_access_opportunities
SET participation_mode = CASE WHEN remote THEN 'REMOTE' ELSE 'PRESENTIAL' END
WHERE participation_mode IS NULL OR participation_mode = '';

UPDATE tb_access_opportunities
SET participation_mode = 'PRESENTIAL'
WHERE participation_mode = 'REMOTE' AND remote = FALSE;

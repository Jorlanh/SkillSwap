-- Validação de responsável para contas de pessoas menores de 18 anos.
-- O fluxo não bloqueia o cadastro, mas registra a pendência e solicita
-- confirmação por link de uso único enviado ao responsável informado.

ALTER TABLE tb_users ADD COLUMN IF NOT EXISTS account_age_band VARCHAR(16) NOT NULL DEFAULT 'ADULT';
ALTER TABLE tb_users ADD COLUMN IF NOT EXISTS minor_access_status VARCHAR(32) NOT NULL DEFAULT 'NOT_APPLICABLE';

ALTER TABLE ss_privacy_consents DROP CONSTRAINT IF EXISTS ck_privacy_consent_purpose;
ALTER TABLE ss_privacy_consents ADD CONSTRAINT ck_privacy_consent_purpose CHECK (
    purpose IN ('PRIVACY_POLICY', 'TERMS_OF_SERVICE', 'OPTIONAL_AI',
                'OPTIONAL_CALENDAR', 'OPTIONAL_VIDEO', 'OPTIONAL_NOTIFICATIONS',
                'MINOR_GUARDIAN_CONSENT')
);

CREATE TABLE IF NOT EXISTS ss_guardian_consents (
    guardian_consent_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES tb_users(user_id) ON DELETE CASCADE,
    guardian_name VARCHAR(120) NOT NULL,
    guardian_email VARCHAR(320) NOT NULL,
    relationship VARCHAR(80) NOT NULL,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    confirmed_at TIMESTAMP WITH TIME ZONE,
    policy_version VARCHAR(32) NOT NULL,
    locale VARCHAR(16) NOT NULL DEFAULT 'pt-BR',
    CONSTRAINT ck_guardian_consent_status CHECK (status IN ('PENDING', 'APPROVED', 'REVOKED', 'EXPIRED'))
);

CREATE INDEX IF NOT EXISTS idx_guardian_consents_user_requested
    ON ss_guardian_consents(user_id, requested_at DESC);

CREATE INDEX IF NOT EXISTS idx_guardian_consents_status_expiry
    ON ss_guardian_consents(status, expires_at);

-- Centro de privacidade: consentimentos versionados e revogáveis.
-- A política completa fica em docs/PRIVACY_POLICY.md e deve ser publicada
-- também na interface antes do uso em produção.

CREATE TABLE IF NOT EXISTS ss_privacy_consents (
    consent_id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES tb_users(user_id) ON DELETE CASCADE,
    purpose VARCHAR(80) NOT NULL,
    policy_version VARCHAR(32) NOT NULL,
    granted BOOLEAN NOT NULL,
    locale VARCHAR(16) NOT NULL DEFAULT 'pt-BR',
    source VARCHAR(32) NOT NULL DEFAULT 'web',
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    withdrawn_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_privacy_consent_purpose CHECK (
        purpose IN ('PRIVACY_POLICY', 'TERMS_OF_SERVICE', 'OPTIONAL_AI',
                    'OPTIONAL_CALENDAR', 'OPTIONAL_VIDEO', 'OPTIONAL_NOTIFICATIONS')
    )
);

ALTER TABLE tb_users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE tb_users ADD COLUMN IF NOT EXISTS accessibility_visual_marker_public BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_privacy_consents_user_purpose
    ON ss_privacy_consents(user_id, purpose, recorded_at DESC);

CREATE INDEX IF NOT EXISTS idx_privacy_consents_recorded_at
    ON ss_privacy_consents(recorded_at);

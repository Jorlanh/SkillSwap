CREATE TABLE IF NOT EXISTS swapcoin_wallets (
    wallet_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    balance BIGINT NOT NULL DEFAULT 0 CHECK (balance >= 0),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_swapcoin_wallet_user UNIQUE (user_id),
    CONSTRAINT fk_swapcoin_wallet_user FOREIGN KEY (user_id) REFERENCES tb_users(user_id)
);

CREATE TABLE IF NOT EXISTS swapcoin_payments (
    payment_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    package_code VARCHAR(32) NOT NULL,
    coin_amount BIGINT NOT NULL CHECK (coin_amount > 0),
    amount_cents BIGINT NOT NULL CHECK (amount_cents > 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    status VARCHAR(16) NOT NULL,
    stripe_checkout_session_id VARCHAR(255),
    stripe_payment_intent_id VARCHAR(255),
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_swapcoin_payment_checkout UNIQUE (stripe_checkout_session_id),
    CONSTRAINT fk_swapcoin_payment_user FOREIGN KEY (user_id) REFERENCES tb_users(user_id),
    CONSTRAINT ck_swapcoin_payment_status CHECK (status IN ('PENDING', 'PAID', 'EXPIRED', 'FAILED'))
);

CREATE TABLE IF NOT EXISTS swapcoin_ledger (
    entry_id BIGSERIAL PRIMARY KEY,
    wallet_id UUID NOT NULL,
    entry_type VARCHAR(32) NOT NULL,
    amount BIGINT NOT NULL CHECK (amount <> 0),
    balance_after BIGINT NOT NULL CHECK (balance_after >= 0),
    description VARCHAR(280) NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_swapcoin_ledger_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_swapcoin_ledger_wallet FOREIGN KEY (wallet_id) REFERENCES swapcoin_wallets(wallet_id),
    CONSTRAINT ck_swapcoin_ledger_type CHECK (entry_type IN ('PURCHASE', 'WELCOME_BONUS', 'TIP_SENT', 'TIP_RECEIVED', 'FEATURE_SPEND', 'REFUND', 'ADJUSTMENT'))
);

CREATE INDEX IF NOT EXISTS idx_swapcoin_ledger_wallet_created ON swapcoin_ledger (wallet_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_swapcoin_payment_user_created ON swapcoin_payments (user_id, created_at DESC);

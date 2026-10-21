CREATE TABLE IF NOT EXISTS ss_organizations (
    organization_id UUID PRIMARY KEY,
    name VARCHAR(180) NOT NULL,
    legal_name VARCHAR(220),
    slug VARCHAR(120) NOT NULL UNIQUE,
    type VARCHAR(40) NOT NULL,
    country VARCHAR(80),
    website VARCHAR(255),
    billing_email VARCHAR(255),
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    plan_code VARCHAR(32) NOT NULL DEFAULT 'TRIAL',
    subscription_status VARCHAR(32) NOT NULL DEFAULT 'TRIAL',
    seat_limit INT NOT NULL DEFAULT 5,
    billing_currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    monthly_price_cents BIGINT NOT NULL DEFAULT 0,
    stripe_customer_id VARCHAR(120),
    stripe_subscription_id VARCHAR(120),
    stripe_checkout_session_id VARCHAR(120),
    created_by UUID NOT NULL REFERENCES tb_users(user_id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ss_organization_memberships (
    membership_id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES ss_organizations(organization_id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES tb_users(user_id) ON DELETE CASCADE,
    role VARCHAR(24) NOT NULL DEFAULT 'MEMBER',
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
    department VARCHAR(120),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_org_membership_user UNIQUE (organization_id, user_id)
);

ALTER TABLE tb_access_opportunities ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE tb_access_opportunities ADD COLUMN IF NOT EXISTS created_by UUID;
ALTER TABLE tb_access_opportunities ADD COLUMN IF NOT EXISTS status VARCHAR(24) NOT NULL DEFAULT 'OPEN';
ALTER TABLE tb_access_opportunities ADD CONSTRAINT fk_access_opportunity_org
    FOREIGN KEY (organization_id) REFERENCES ss_organizations(organization_id) ON DELETE SET NULL;
ALTER TABLE tb_access_opportunities ADD CONSTRAINT fk_access_opportunity_creator
    FOREIGN KEY (created_by) REFERENCES tb_users(user_id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_org_membership_user ON ss_organization_memberships(user_id, status);
CREATE INDEX IF NOT EXISTS idx_org_membership_org ON ss_organization_memberships(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_access_opportunities_org ON tb_access_opportunities(organization_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_access_opportunities_status ON tb_access_opportunities(status, created_at DESC);

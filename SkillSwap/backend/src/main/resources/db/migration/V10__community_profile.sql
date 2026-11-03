ALTER TABLE tb_communities ADD COLUMN IF NOT EXISTS image_url VARCHAR(1024);
ALTER TABLE tb_communities ADD COLUMN IF NOT EXISTS owner_id UUID;
ALTER TABLE tb_communities DROP CONSTRAINT IF EXISTS fk_communities_owner;
ALTER TABLE tb_communities ADD CONSTRAINT fk_communities_owner
    FOREIGN KEY (owner_id) REFERENCES tb_users(user_id) ON DELETE SET NULL;

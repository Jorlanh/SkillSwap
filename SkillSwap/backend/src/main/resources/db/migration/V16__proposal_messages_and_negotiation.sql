ALTER TABLE tb_skill_swap_proposals
    ADD COLUMN IF NOT EXISTS message TEXT;

ALTER TABLE tb_skill_swap_proposals
    ADD COLUMN IF NOT EXISTS counter_message TEXT;

DO $$
DECLARE
    existing_constraint TEXT;
BEGIN
    SELECT tc.constraint_name
      INTO existing_constraint
      FROM information_schema.table_constraints tc
     WHERE tc.table_schema = current_schema()
       AND tc.table_name = 'tb_skill_swap_proposals'
       AND tc.constraint_type = 'CHECK'
       AND tc.constraint_name ILIKE '%status%'
     LIMIT 1;

    IF existing_constraint IS NOT NULL THEN
        EXECUTE format('ALTER TABLE tb_skill_swap_proposals DROP CONSTRAINT %I', existing_constraint);
    END IF;
END $$;

ALTER TABLE tb_skill_swap_proposals
    ADD CONSTRAINT ck_skill_swap_proposals_status
    CHECK (status IN ('PENDING', 'NEGOTIATING', 'ACCEPTED', 'REJECTED', 'BLOCKED', 'COMPLETED'));

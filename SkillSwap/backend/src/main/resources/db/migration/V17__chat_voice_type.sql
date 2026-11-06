ALTER TABLE tb_chat_messages
    ADD COLUMN IF NOT EXISTS voice_type VARCHAR(255);

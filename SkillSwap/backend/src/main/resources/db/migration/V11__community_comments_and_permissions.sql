ALTER TABLE tb_communities ADD COLUMN IF NOT EXISTS is_private BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE tb_community_members ADD COLUMN IF NOT EXISTS member_role VARCHAR(20) NOT NULL DEFAULT 'MEMBER';
ALTER TABLE tb_community_members ADD COLUMN IF NOT EXISTS membership_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED';

ALTER TABLE tb_comments ADD COLUMN IF NOT EXISTS parent_comment_id BIGINT;
ALTER TABLE tb_comments DROP CONSTRAINT IF EXISTS fk_comments_parent;
ALTER TABLE tb_comments ADD CONSTRAINT fk_comments_parent
    FOREIGN KEY (parent_comment_id) REFERENCES tb_comments(comment_id) ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS tb_comment_likes (
    comment_id BIGINT NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (comment_id, user_id),
    FOREIGN KEY (comment_id) REFERENCES tb_comments(comment_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES tb_users(user_id) ON DELETE CASCADE
);

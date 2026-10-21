/** -- Tabela de papéis (roles)
CREATE TABLE IF NOT EXISTS tb_roles (
    role_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL
);

-- Tabela de usuários
CREATE TABLE IF NOT EXISTS tb_users (
    user_id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    birth_date DATE,
    phone_number VARCHAR(20),
    verification_code VARCHAR(6),
    verification_code_expiry TIMESTAMP,
    verified BOOLEAN DEFAULT FALSE,
    verified_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    two_factor_secret VARCHAR(255),
    bio TEXT,
    country VARCHAR(100),
    city VARCHAR(100),
    state VARCHAR(100),
    followers INT DEFAULT 0
);

-- Tabela de perfis
CREATE TABLE IF NOT EXISTS tb_profiles (
    profile_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    description TEXT,
    image_url VARCHAR(255),
    location VARCHAR(255),
    contact_info VARCHAR(255),
    social_media_links TEXT,
    availability_status VARCHAR(50),
    experience_level VARCHAR(255),
    education_level VARCHAR(255),
    interests TEXT,
    FOREIGN KEY (user_id) REFERENCES tb_users(user_id) ON DELETE CASCADE
);

-- Tabela de habilidades (skills)
CREATE TABLE IF NOT EXISTS tb_skills (
    skill_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(255),
    level VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    profile_id UUID,
    FOREIGN KEY (profile_id) REFERENCES tb_profiles(profile_id) ON DELETE CASCADE
);

-- Tabela de associação entre usuários e papéis
CREATE TABLE IF NOT EXISTS tb_user_roles (
    user_id UUID NOT NULL,
    role_id INT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES tb_users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES tb_roles(role_id) ON DELETE CASCADE
);

-- Tabela de associação entre usuários e habilidades
CREATE TABLE IF NOT EXISTS tb_user_skills (
    user_id UUID NOT NULL,
    skill_id INT NOT NULL,
    PRIMARY KEY (user_id, skill_id),
    FOREIGN KEY (user_id) REFERENCES tb_users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id) REFERENCES tb_skills(skill_id) ON DELETE CASCADE
);

-- Tabela de comunidades
CREATE TABLE IF NOT EXISTS tb_communities (
    community_id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabela de associação entre comunidades e membros
CREATE TABLE IF NOT EXISTS tb_community_members (
    community_id UUID NOT NULL,
    user_id UUID NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (community_id, user_id),
    FOREIGN KEY (community_id) REFERENCES tb_communities(community_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES tb_users(user_id) ON DELETE CASCADE
);

-- Tabela de postagens
CREATE TABLE IF NOT EXISTS tb_posts (
    post_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    content TEXT NOT NULL,
    title VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    profile_id UUID NOT NULL,
    community_id UUID,
    image_url VARCHAR(255),
    video_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    likes_count INT DEFAULT 0,
    reposts_count INT DEFAULT 0,
    comments_count INT DEFAULT 0,
    shares_count INT DEFAULT 0,
    views_count INT DEFAULT 0,
    repost_of BIGINT,
    share_url VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES tb_users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (profile_id) REFERENCES tb_profiles(profile_id) ON DELETE CASCADE,
    FOREIGN KEY (community_id) REFERENCES tb_communities(community_id) ON DELETE CASCADE,
    FOREIGN KEY (repost_of) REFERENCES tb_posts(post_id) ON DELETE SET NULL
);

-- Tabela de comentários
CREATE TABLE IF NOT EXISTS tb_comments (
    comment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id UUID NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES tb_posts(post_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES tb_users(user_id) ON DELETE CASCADE
);

-- Tabela de curtidas (likes)
CREATE TABLE IF NOT EXISTS tb_likes (
    post_id BIGINT NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (post_id, user_id),
    FOREIGN KEY (post_id) REFERENCES tb_posts(post_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES tb_users(user_id) ON DELETE CASCADE
);

-- Tabela de repostagens
CREATE TABLE IF NOT EXISTS tb_reposts (
    post_id BIGINT NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (post_id, user_id),
    FOREIGN KEY (post_id) REFERENCES tb_posts(post_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES tb_users(user_id) ON DELETE CASCADE
);

-- Tabela de links de compartilhamento
CREATE TABLE IF NOT EXISTS tb_share_links (
    share_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    user_id UUID NOT NULL,
    share_url VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    click_count INT DEFAULT 0,
    FOREIGN KEY (post_id) REFERENCES tb_posts(post_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES tb_users(user_id) ON DELETE CASCADE
);

-- Tabela de lições
CREATE TABLE IF NOT EXISTS tb_lessons (
    lesson_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    teacher_id UUID NOT NULL,
    student_id UUID NOT NULL,
    scheduled_time TIMESTAMP WITH TIME ZONE,
    status VARCHAR(50),
    FOREIGN KEY (teacher_id) REFERENCES tb_users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES tb_users(user_id) ON DELETE CASCADE
);

-- Tabela de notificações
CREATE TABLE IF NOT EXISTS tb_notifications (
    notification_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id UUID NOT NULL,
    message TEXT NOT NULL,
    sent_at TIMESTAMP NOT NULL,
    read BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES tb_users(user_id) ON DELETE CASCADE
);

-- Tabela de mensagens
CREATE TABLE IF NOT EXISTS tb_messages (
    message_id UUID PRIMARY KEY,
    sender_id UUID NOT NULL,
    receiver_id UUID NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(50) NOT NULL CHECK (type IN ('text', 'image', 'video', 'pdf')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES tb_users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES tb_users(user_id) ON DELETE CASCADE
);

-- Tabela de propostas de troca de habilidades
CREATE TABLE IF NOT EXISTS tb_skill_swap_proposals (
    proposal_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sender_id UUID NOT NULL,
    receiver_id UUID NOT NULL,
    offered_skill_id INT NOT NULL,
    requested_skill_id INT NOT NULL,
    status VARCHAR(50) NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'BLOCKED')),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES tb_users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES tb_users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (offered_skill_id) REFERENCES tb_skills(skill_id) ON DELETE CASCADE,
    FOREIGN KEY (requested_skill_id) REFERENCES tb_skills(skill_id) ON DELETE CASCADE
);

-- Tabela para armazenar permissões de usuários
CREATE TABLE IF NOT EXISTS tb_permissions (
    permission_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id UUID NOT NULL,
    permission_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ALLOWED', 'ALLOWED_DURING_USE', 'DENIED', 'NOT_SET')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES tb_users(user_id) ON DELETE CASCADE,
    UNIQUE (user_id, permission_type)
);

-- Tabela para armazenar o histórico de acessos
CREATE TABLE IF NOT EXISTS tb_access_logs (
    access_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id UUID NOT NULL,
    access_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    location VARCHAR(255),
    city VARCHAR(100),
    subdivision VARCHAR(100),
    country VARCHAR(100),
    FOREIGN KEY (user_id) REFERENCES tb_users(user_id) ON DELETE CASCADE
);

-- Tabela para armazenar configurações de autenticação em duas etapas
CREATE TABLE IF NOT EXISTS tb_two_factor_configs (
    config_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id UUID NOT NULL,
    method VARCHAR(50) NOT NULL CHECK (method IN ('GOOGLE_AUTH', 'EMAIL', 'SMS')),
    enabled BOOLEAN DEFAULT FALSE,
    secret VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES tb_users(user_id) ON DELETE CASCADE,
    UNIQUE (user_id, method)
);

-- Dados iniciais
-- INSERT INTO tb_roles (role_id, name) VALUES (1, 'USER');
-- INSERT INTO tb_roles (role_id, name) VALUES (2, 'ADMIN');

-- INSERT INTO tb_users (user_id, username, password, name, email, created_at, bio, country, city, state) VALUES
-- (UUID_TO_BIN('123e4567-e89b-12d3-a456-426614174000'), 'professor', '$2a$10$examplehashedpassword', 'Professor Exemplo', 'professor@exemplo.com', CURRENT_TIMESTAMP, 'Apaixonado por ensinar', 'Brasil', 'Salvador', 'Bahia'),
-- (UUID_TO_BIN('987e6543-e21b-12d3-a456-426614174000'), 'aluno', '$2a$10$examplehashedpassword', 'Aluno Exemplo', 'aluno@exemplo.com', CURRENT_TIMESTAMP, 'Em busca de aprendizado', 'Brasil', 'Salvador', 'Bahia');

-- Índices para melhorar desempenho
CREATE INDEX idx_access_logs_user_id ON tb_access_logs(user_id);
CREATE INDEX idx_permissions_user_id ON tb_permissions(user_id);
CREATE INDEX idx_users_email ON tb_users(email);
CREATE INDEX idx_posts_user_id ON tb_posts(user_id);
CREATE INDEX idx_posts_community_id ON tb_posts(community_id);
CREATE INDEX idx_comments_post_id ON tb_comments(post_id);
CREATE INDEX idx_likes_post_id ON tb_likes(post_id);
CREATE INDEX idx_reposts_post_id ON tb_reposts(post_id);
CREATE INDEX idx_share_links_post_id ON tb_share_links(post_id);
CREATE INDEX idx_two_factor_configs_user_id ON tb_two_factor_configs(user_id);
CREATE INDEX idx_messages_sender_id ON tb_messages(sender_id);
CREATE INDEX idx_messages_receiver_id ON tb_messages(receiver_id); - */
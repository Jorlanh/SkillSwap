-- Promove um usuário Firebase já existente para ADMIN.
-- Uso seguro (psql): psql "$DB_URL" -v ADMIN_EMAIL="admin@dominio.com" -f promote-admin.sql
-- A senha nunca é armazenada nesta tabela: a autenticação permanece no Firebase.

BEGIN;

INSERT INTO tb_roles (name)
SELECT 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM tb_roles WHERE UPPER(name) = 'ADMIN');

INSERT INTO tb_user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM tb_users u
JOIN tb_roles r ON UPPER(r.name) = 'ADMIN'
WHERE LOWER(u.email) = LOWER(:'ADMIN_EMAIL')
ON CONFLICT (user_id, role_id) DO NOTHING;

COMMIT;

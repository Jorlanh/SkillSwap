CREATE TABLE IF NOT EXISTS tb_access_opportunities (
    opportunity_id UUID PRIMARY KEY,
    organization_name VARCHAR(160) NOT NULL,
    title VARCHAR(180) NOT NULL,
    description TEXT NOT NULL,
    required_skills TEXT NOT NULL,
    remote BOOLEAN NOT NULL DEFAULT FALSE,
    accessible_support BOOLEAN NOT NULL DEFAULT FALSE,
    location VARCHAR(120),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO tb_access_opportunities (opportunity_id, organization_name, title, description, required_skills, remote, accessible_support, location)
VALUES
 ('3d8b1d57-96d3-4b18-b55c-2c43db28a101', 'SkillSwap Lab', 'Projeto aplicado de acessibilidade digital', 'Desafio colaborativo para transformar uma jornada real em uma experiência web mais acessível.', 'acessibilidade, react, pesquisa', TRUE, TRUE, 'Global'),
 ('c4f6af77-1d52-44a1-9b3e-7cb6e3b7a102', 'Instituto Conecta', 'Trilha de desenvolvimento web inclusivo', 'Formação com atividades práticas, mentoria e comprovação de habilidades.', 'javascript, spring, comunicação', TRUE, TRUE, 'Brasil'),
 ('7a214c21-7b5f-4fbf-8ba6-1ddc07f9a103', 'Empresa Parceira', 'Banco de talentos SkillSwap Access', 'Oportunidade para pessoas que desejam demonstrar habilidades por projetos e desafios.', 'java, postgres, trabalho em equipe', FALSE, TRUE, 'Salvador/BA')
ON CONFLICT (opportunity_id) DO NOTHING;

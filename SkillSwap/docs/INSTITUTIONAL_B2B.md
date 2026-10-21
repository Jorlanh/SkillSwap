# Institutional Workspace B2B2C

## Objetivo

O Institutional Workspace transforma o SkillSwap em uma plataforma institucional para universidades, empresas, governos, ONGs e organizações de capacitação. A experiência individual continua no `/acesso`; a experiência organizacional fica no `/institucional`.

## Fluxo entregue

`organização -> membros e papéis -> plano de assentos -> oportunidades -> candidaturas -> indicadores agregados -> cobrança institucional`

### Organização e isolamento

- cada organização possui identidade, slug, tipo, país, site e e-mail de cobrança;
- memberships ligam usuários existentes a uma organização;
- papéis institucionais: `OWNER`, `ADMIN`, `MANAGER`, `ANALYST` e `MEMBER`;
- todas as rotas institucionais exigem membership ativo na organização da URL;
- uma conta pode pertencer a várias organizações;
- o limite de assentos é validado ao vincular membros e alterar planos.

### Oportunidades e candidaturas

- gestores publicam oportunidades em nome da própria organização;
- oportunidades possuem `DRAFT`, `OPEN`, `CLOSED` e `ARCHIVED`;
- apenas oportunidades `OPEN` aparecem na descoberta pública e aceitam candidaturas;
- gestores e analistas acompanham candidaturas da organização;
- candidaturas possuem `SUBMITTED`, `REVIEWING`, `ACCEPTED`, `REJECTED` e `WITHDRAWN`.

### Dashboard e relatórios

O dashboard institucional apresenta membros ativos, uso de assentos, oportunidades abertas, candidaturas, planos de evolução, evidências e distribuição do funil de candidaturas. Os dados são agregados apenas entre membros e oportunidades daquela organização.

### Planos e cobrança

O catálogo inicial usa `TRIAL`, `TEAM`, `GROWTH` e `ENTERPRISE`, com assentos, preço mensal regional e três moedas: `BRL` para o Brasil, `EUR` para organizações europeias e `USD` para os EUA e mercados dolarizados. Os preços de referência são Team `R$ 249`, `€ 45` ou `$ 49`; Growth `R$ 999`, `€ 185` ou `$ 199`; Enterprise `R$ 3.999`, `€ 749` ou `$ 799` por mês. O plano `TRIAL` permanece gratuito.

Ao criar uma organização, o país informado sugere a moeda regional. O gestor autorizado pode confirmar ou alterar a moeda no painel institucional antes de salvar ou iniciar o checkout. Organizações existentes preservam a moeda já cadastrada.

O checkout recorrente existe como extensão de código, mas permanece desativado no modo gratuito padrão (`APP_COMMERCE_ENABLED=false`). A equipe não cobra os usuários nesta entrega. Caso uma futura decisão de produto altere isso, será necessário revisar política, contratos, bases legais e comunicação antes de ativar a Stripe.

O modo piloto pode atualizar o plano manualmente. A configuração da Stripe não é necessária para a operação gratuita. Não publique chaves no frontend.

## Contratos HTTP

- `GET /api/institutional/catalog`
- `GET|POST /api/institutional/organizations`
- `GET /api/institutional/organizations/{id}/dashboard`
- `GET|POST /api/institutional/organizations/{id}/members`
- `PATCH|DELETE /api/institutional/organizations/{id}/members/{membershipId}`
- `GET|POST /api/institutional/organizations/{id}/opportunities`
- `PATCH /api/institutional/organizations/{id}/opportunities/{opportunityId}/status`
- `GET /api/institutional/organizations/{id}/applications`
- `PATCH /api/institutional/organizations/{id}/applications/{applicationId}/status`
- `GET|PUT /api/institutional/organizations/{id}/subscription`
- `POST /api/institutional/organizations/{id}/billing/checkout`

## Integrações de empresa

A camada de autorização e os contratos institucionais estão prontos para receber SSO, LMS, ATS/RH e provisionamento por diretório. Esses conectores não são inventados como se já estivessem homologados: cada cliente deve fornecer o provedor, os escopos, o contrato de dados e o ambiente de teste. O próximo passo de uma implantação corporativa é adicionar o conector escolhido, preferencialmente por OIDC/SAML para SSO e SCIM ou convite controlado para provisionamento.

## Segurança e operação

- organização é resolvida pela URL, mas acesso é sempre conferido pela membership do usuário autenticado;
- papéis globais `ADMIN` não substituem a checagem institucional nas rotas do workspace;
- o checkout só usa chaves server-side e metadata mínima;
- o webhook Stripe verifica assinatura antes de aplicar a ativação;
- o domínio de uma oportunidade institucional é mantido por foreign key e não depende apenas de `organizationName`;
- dados de impacto devem ser apresentados de forma agregada e autorizada, sem transformar recomendação em decisão automática de contratação.

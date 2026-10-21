# SkillSwap

Plataforma web de aprendizagem acessível, troca de habilidades, comprovação prática e conexão com oportunidades profissionais. O produto atende usuários comuns e mantém foco prioritário em pessoas com deficiência, sem separar a experiência principal.

Declaração de autoria: a equipe utilizou inteligência artificial somente para correção ortográfica, pontuação e revisão formal. Requisitos, decisões de produto, arquitetura, código, referências, dados e validação são responsabilidade dos cinco autores e devem ser conferidos pela equipe e pelo orientador.

## Escopo final

Esta entrega é exclusivamente de software. A alternativa de hardware foi retirada do TCC e não é requisito, dependência, protótipo, desenho ou promessa desta versão.

## Produto e arquitetura

- frontend React, TypeScript e Vite;
- backend Spring Boot em monólito modular;
- PostgreSQL com JPA e migrações Flyway;
- Firebase Authentication para identidade e Firebase Cloud Messaging para notificações quando habilitado;
- Gemini 2.5 Flash (`gemini-2.5-flash`) no backend para chatbot, tradução, simplificação e moderação;
- recomendações determinísticas com pontuação e explicação;
- módulo `/acesso` como jornada completa de oportunidades: plano, etapas, projeto, evidência e candidatura;
- segurança por JWT validado no servidor, autorização por papel, WebSockets autenticados na primeira mensagem e ausência de token em query string;
- Actuator e Prometheus para métricas técnicas; Grafana é opcional;
- Railway como alvo de hospedagem do MVP, usando serviços gratuitos ou créditos institucionais quando disponíveis.
- contas de pessoas menores de 18 anos com confirmação de responsável por link de uso único; o acesso permanece disponível enquanto o status estiver pendente.
- plataforma gratuita para os usuários: `APP_COMMERCE_ENABLED=false` por padrão; SwapCoin e checkout não são exigidos no uso comum;
- modo de alto contraste com foco e interativos destacados;
- marcador visual opcional de cor e forma, escolhido pelo próprio usuário;
- conclusão de aulas com certificado imprimível contendo aluno, carga horária, introdução e código único.

## Módulo SkillSwap Access

A tela autenticada `/acesso` apresenta o dashboard da jornada e consome os contratos de `GET/POST/PATCH /api/opportunity-os/**`, além das recomendações e oportunidades existentes. O cadastro de oportunidades por `POST /api/access/opportunities` exige `ADMIN`. A migração `V5__skillswap_access.sql` cria a base de oportunidades e `V6__opportunity_os.sql` cria plano, etapas, evidências e candidaturas.

A recomendação inicial compara habilidades e interesses declarados com os requisitos da oportunidade. A resposta apresenta pontuação, fatores e apoio de acessibilidade. Isso é um mecanismo explicável para cold start, não uma decisão automatizada de contratação e não uma prova de superioridade antes da validação.

O fluxo agora fecha o ciclo: o usuário define uma meta, executa quatro etapas, registra uma entrega verificável, solicita revisão e se candidata a um desafio. A evidência continua autodeclarada até passar por revisão; a plataforma não inventa certificações nem promete emprego.

## Execução local

1. Copie `.env.example` para um arquivo local e preencha somente variáveis privadas.
2. Copie `frontend/.env.example` para `frontend/.env.local`.
3. Suba PostgreSQL e, se necessário, Redis.
4. Execute `backend/mvnw.cmd spring-boot:run`.
5. Execute `npm ci` e `npm run dev` em `frontend`.

Em um deploy real, configure os segredos no Railway. Nunca publique `.env`, `properties.txt`, `Livekit.txt`, chaves privadas, tokens FCM ou segredos Stripe. As credenciais compartilhadas durante a preparação devem ser revogadas e substituídas antes de qualquer uso externo.

## Documentação

- [Moderação assistida, decisões humanas e recursos](docs/MODERACAO_ASSISTIDA_E_RECURSOS.md)

- `docs/SKILLSWAP_ACCESS_SPEC.md`: requisitos e proposta de valor;
- `docs/ARCHITECTURE.md`: arquitetura e diagrama de software;
- `docs/IEEE_DIFFERENTIAL.md`: estado da arte, referências IEEE e hipótese de diferencial;
- `docs/DEPLOYMENT_CHECKLIST.md`: checklist do Railway e opções gratuitas;
- `docs/PROMETHEUS.md`: finalidade da observabilidade;
- `docs/GEMINI_CONFIGURATION.md`: configuração segura do Gemini 2.5 Flash;
- `docs/ACCESSIBILITY_MARKERS_AND_CERTIFICATES.md`: alto contraste, marcadores visuais e certificados;
- `docs/OPPORTUNITY_OS.md`: contratos, jornada e limites do módulo de oportunidades;
- `docs/INSTITUTIONAL_B2B.md`: Workspace institucional B2B2C, multi-organizacao, RBAC, dashboard, oportunidades e Stripe;
- `docs/SECURITY_ROTATION_REQUIRED.md`: resposta a credenciais expostas;
- `docs/PRIVACY_POLICY.md`, `docs/DATA_INVENTORY_AND_TRANSFERS.md`, `docs/RETENTION_POLICY.md`, `docs/INCIDENT_RESPONSE.md` e `docs/PRIVACY_IMPLEMENTATION_STATUS.md`: privacidade, retenção, fornecedores, incidentes e controles implementados;
- `docs/LEGAL_REVIEW_CHECKLIST.md`: decisões que precisam ser revisadas antes da publicação;
- `docs/GOVERNANCE_CONTACTS.md`, `docs/LEGAL_BASES_MATRIX.md` e `docs/DPIA_RIPD.md`: controlador, DPO, bases legais e análise de impacto;
- `docs/MINOR_SAFETY_POLICY.md`: salvaguardas para menores, chat, vídeo e regras por país;
- `docs/PROVIDER_DPA_AND_TRANSFER_REGISTER.md` e `docs/EXTERNAL_DELETION_AND_BACKUP_RUNBOOK.md`: fornecedores, transferências, backups e exclusão;
- `docs/ACCESSIBILITY_SECURITY_TEST_PLAN.md` e `docs/PRODUCTION_VALIDATION_RUNBOOK.md`: evidências que ainda precisam ser executadas;
- `docs/tcc/SkillSwap_Access_TCC.docx` e `.pdf`: documentação acadêmica;
- `docs/tcc/SkillSwap_TCC_Overleaf`: fonte no modelo de artigo UCSAL;
- `docs/tcc/CARTA_PROPOSTA_EQUIPE.md` e `.docx`: proposta para aprovação da equipe.

## Camada institucional B2B2C

Esta versao inclui o modulo `/institucional` para uso institucional. Ele adiciona organizacoes, memberships, papeis `OWNER`, `ADMIN`, `MANAGER`, `ANALYST` e `MEMBER`, isolamento por organizacao, dashboard agregado, gestao de membros, oportunidades e candidaturas. O checkout recorrente Stripe permanece desativado no modo gratuito padrão.

A migracao `V7__institutional_workspace.sql` e a fonte de verdade do novo dominio. SSO, LMS, ATS/RH, SCIM e conectores academicos possuem contratos e pontos de extensao documentados, mas nao sao apresentados como integracoes homologadas nesta entrega.

O modelo institucional mantém campos de moeda e plano para evolução futura, mas a cobrança não é oferecida aos usuários nesta entrega. A moeda e o checkout só devem ser ativados mediante decisão separada da equipe.

## Limites honestos da entrega

O pacote contém implementação e contratos, mas integração com Firebase, PostgreSQL, Railway, Stripe, vídeo e notificações ainda precisa de credenciais de homologação e evidência reproduzível. Não foram inventados resultados de usuários, métricas de impacto ou certificações.

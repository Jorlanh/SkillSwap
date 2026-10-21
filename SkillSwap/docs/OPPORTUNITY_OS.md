# SkillSwap Opportunity OS

## Objetivo

O módulo transforma uma intenção em uma sequência verificável:

`objetivo -> plano -> prática -> evidência -> candidatura -> oportunidade`

Ele complementa o SkillSwap existente sem substituir comunidades, propostas, aulas, chat, reputação ou carteira.

## Jornada implementada

1. O usuário cria um plano com objetivo, direção profissional, limitações e horas semanais.
2. A API gera quatro etapas persistidas: resultado, fundamentos, projeto real e evidência.
3. Cada etapa pode ser marcada como `TODO`, `IN_PROGRESS` ou `DONE`.
4. O usuário registra uma evidência com título, habilidade, descrição, tipo e link opcional.
5. Uma evidência pode ser relacionada a uma etapa ou oportunidade e pode solicitar revisão humana.
6. O usuário se candidata a uma oportunidade e acompanha o status da candidatura.

## Rotas autenticadas

- `GET /api/opportunity-os/dashboard`
- `POST /api/opportunity-os/plan`
- `PATCH /api/opportunity-os/plan/steps/{stepId}`
- `GET /api/opportunity-os/evidence`
- `POST /api/opportunity-os/evidence`
- `POST /api/opportunity-os/evidence/{evidenceId}/review`
- `GET /api/opportunity-os/applications`
- `POST /api/opportunity-os/opportunities/{opportunityId}/apply`

A tela `/acesso` apresenta o dashboard completo. `/jornada` é um alias para a mesma experiência.

## Decisões de produto

- A primeira geração do plano é determinística e baseada no perfil para funcionar sem depender de uma chave externa.
- O ELOS/Gemini continua disponível para orientação conversacional e refinamento textual.
- Evidência autodeclarada não é apresentada como certificação. A revisão muda o estado para `REVIEW_REQUESTED` e exige validação humana ou institucional.
- O matching existente continua explicável e não decide contratação automaticamente.
- A candidatura é um registro de interesse; não representa promessa de emprego, renda ou aprovação.

## Persistência

A migração `V6__opportunity_os.sql` cria `ss_growth_plans`, `ss_growth_plan_steps`, `ss_skill_evidence` e `ss_opportunity_applications`. Flyway continua sendo a fonte de verdade.

# Alinhamento entre requisitos e código

| Requisito | Evidência no código | Estado |
|---|---|---|
| Frontend React | `frontend/src/App.tsx`, páginas e serviços | implementado |
| Identidade Firebase | SDK frontend e Resource Server Spring | implementado, depende de configuração |
| Perfil e habilidades | entidades, serviços e páginas existentes | implementado |
| Módulo Access | `AccessPage.tsx`, `AccessService`, `AccessController` | incrementado |
| Oportunidades | `AccessOpportunity`, repository e V5 Flyway | incrementado |
| Jornada de plano | `OpportunityOsPage.tsx`, `OpportunityOsController`, `GrowthPlan` | implementado |
| Etapas práticas | `GrowthPlanStep`, `V6__opportunity_os.sql`, atualização de status | implementado |
| Evidências de portfólio | `SkillEvidence`, formulário de evidência e pedido de revisão | implementado como autodeclaração com revisão pendente |
| Candidaturas | `OpportunityApplication`, `OpportunityOsService`, botão de candidatura | implementado |
| Recomendação explicável | `AccessRecommendationDTO` e regra determinística | implementado como MVP |
| FCM | `DeviceController` e `messaging.ts` | condicional à configuração |
| Chat seguro | `ChatWebSocketHandler` e primeiro frame autenticado | incrementado |
| PostgreSQL | JPA e migrations V1–V5 | implementado |
| Prometheus | Actuator endpoint documentado | condicional à exposição segura |
| Hardware | nenhum | fora do escopo |

O estado “condicional” não é apresentado como prova de produção. Firebase, Railway, banco, Stripe, FCM, vídeo e observabilidade precisam de homologação com credenciais privadas.

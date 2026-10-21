# Registro de operadores, contratos e transferências

Nenhum fornecedor deve ser considerado aprovado apenas porque tem plano gratuito. A equipe deve preencher este registro com os termos vigentes, DPA, subprocessadores, região de processamento, retenção e procedimento de exclusão.

| Fornecedor | Uso no SkillSwap | Dados possíveis | Região/subprocessadores | DPA/transferência | Exclusão testada | Estado |
|---|---|---|---|---|---|---|
| Firebase Authentication/FCM | identidade e notificações | e-mail, identificador, token FCM | PREENCHER | PREENCHER | PENDENTE | revisar |
| Railway/PostgreSQL | aplicação e banco | conta, perfil, aula, chat, consentimento | PREENCHER | PREENCHER | PENDENTE | revisar |
| Google Calendar | eventos de aula | e-mails, horário, título | PREENCHER | PREENCHER | PENDENTE | opcional |
| Gemini | chatbot, tradução e moderação | texto enviado à funcionalidade | PREENCHER | PREENCHER | PENDENTE | opcional |
| Groq | fallback de IA | texto enviado à funcionalidade | PREENCHER | PREENCHER | PENDENTE | opcional |
| LiveKit | sala de vídeo | identidade e metadados da sala | PREENCHER | PREENCHER | PENDENTE | opcional |
| Cloudinary | arquivos/mídia, se habilitado | imagens e arquivos | PREENCHER | PREENCHER | PENDENTE | opcional |
| SMTP | e-mails de conta e responsável | endereço e conteúdo de aviso | PREENCHER | PREENCHER | PENDENTE | necessário para responsável |
| Redis | rate limit e sessão distribuída | chaves técnicas e sessão | PREENCHER | PREENCHER | PENDENTE | opcional |

## Critério de ativação

Uma integração só pode ser habilitada no Railway quando houver: finalidade registrada, consentimento ou base legal definida, contrato/DPA revisado, transferência documentada, prazo de retenção, procedimento de exclusão e teste de erro/fallback.

## Transferência internacional

Registrar país de destino, decisão de adequação quando aplicável, cláusulas contratuais ou outro mecanismo reconhecido, medidas suplementares, subprocessadores e data da revisão. Não afirmar que o uso de HTTPS sozinho resolve transferência internacional.

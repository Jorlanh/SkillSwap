# Runbook de exclusão, backups e fornecedores

`DELETE /api/privacy/me` anonimiza a conta local. Isso não prova exclusão imediata em caches, backups, Firebase, SMTP, LiveKit ou outros operadores. O procedimento abaixo deve ser executado e registrado quando uma conta real solicitar exclusão.

## Ordem de execução

1. confirmar a identidade do solicitante sem registrar documentos desnecessários;
2. exportar os dados se o titular solicitar antes da exclusão;
3. executar a anonimização local e confirmar `deleted_at`;
4. revogar tokens, sessões, consentimentos opcionais e solicitações pendentes;
5. solicitar exclusão nos fornecedores que receberam dados;
6. registrar protocolo, prazo contratual e resposta de cada fornecedor;
7. verificar política de backups: expiração, sobrescrita, restauração e bloqueio de reintrodução;
8. fazer teste de consulta por e-mail, identificador e hash do titular;
9. responder ao titular com o que foi removido, o que permanece por obrigação e o prazo aplicável.

## Evidência mínima

| Sistema | Dado enviado | Solicitação/protocolo | Prazo informado | Resposta | Verificação | Responsável |
|---|---|---|---|---|---|---|
| SkillSwap/PostgreSQL | conta e relações | PREENCHER | PREENCHER | PREENCHER | consulta sem identificador | PREENCHER |
| Firebase | identidade e token | PREENCHER | PREENCHER | PREENCHER | conta inexistente | PREENCHER |
| Google Calendar | evento | PREENCHER | PREENCHER | PREENCHER | evento removido | PREENCHER |
| LiveKit | metadados de sala | PREENCHER | PREENCHER | PREENCHER | painel/API | PREENCHER |
| IA | conteúdo enviado | PREENCHER | PREENCHER | PREENCHER | política de retenção | PREENCHER |
| mídia/SMTP | arquivo ou endereço | PREENCHER | PREENCHER | PREENCHER | API/painel | PREENCHER |
| backups | cópia técnica | PREENCHER | PREENCHER | PREENCHER | restauração controlada | PREENCHER |

## Regra de segurança

Não restaurar um backup antigo diretamente sobre produção. Se uma restauração for necessária, aplicar a anonimização registrada ou impedir a reintrodução do usuário excluído antes de disponibilizar os dados.

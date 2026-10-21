# Checklist de deploy econômico no Railway

## Antes de publicar

- revogar e rotacionar todas as credenciais que apareceram nos arquivos originais ou em mensagens;
- não subir `.env`, `properties.txt`, `Livekit.txt`, chaves privadas nem tokens;
- habilitar autenticação Firebase, domínios autorizados e política de recuperação;
- criar PostgreSQL Railway e executar apenas Flyway;
- configurar `CORS_ALLOWED_ORIGINS` e `APP_FRONTEND_URL` com domínios reais;
- configurar SMTP de produção para o link de confirmação de responsável (`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` e flags SMTP);
- definir `APP_COMMERCE_ENABLED=false` para manter a plataforma gratuita;
- deixar `GOOGLE_CALENDAR_ENABLED=false` até credencial, consentimento e calendário compartilhado serem homologados;
- não configurar Stripe no Railway enquanto a decisão for não cobrar usuários;
- preencher os contatos de controlador/DPO no Railway e remover os valores padrão “a definir”;
- aprovar `LEGAL_BASES_MATRIX.md`, `DPIA_RIPD.md` e `MINOR_SAFETY_POLICY.md`;
- concluir DPA, transferência e exclusão dos fornecedores listados em `PROVIDER_DPA_AND_TRANSFER_REGISTER.md`;
- executar `PRODUCTION_VALIDATION_RUNBOOK.md` e guardar evidências;
- decidir se FCM, LiveKit, Redis e RabbitMQ serão usados; recursos não configurados devem ter fallback.
- aplicar a migration `V11__assisted_moderation_queue.sql` e manter a contenção automática de conta desligada até a equipe aprovar o procedimento de revisão;
- testar a fila de moderação com duas contas de teste, uma decisão humana, um recurso e a expiração de uma suspensão temporária;

## Plano gratuito ou de baixo custo

O MVP pode usar o plano institucional/gratuito disponível no Railway, Firebase Authentication, FCM, Leaflet/OpenStreetMap ou MapLibre para mapas, Web Speech API, texto/arquivos como fallback de vídeo e Caffeine no lugar de Redis quando a carga permitir. Mapas, vídeo, SMS e pagamentos não devem ser declarados como obrigatórios se não forem configurados. SMS foi retirado do caminho principal; o sistema usa e-mail/Firebase e notificações FCM quando habilitadas.

## Sequência

1. Criar banco vazio.
2. Configurar segredos no Railway.
3. Publicar backend e validar `/actuator/health`.
4. Publicar frontend e restringir CORS.
5. Configurar domínio no Firebase.
6. Executar cadastro, login, perfil, troca, chat, Access, notificação e administração.
7. Testar consentimento, exportação, correção, revogação, desativação e cadastro de menor com confirmação de responsável em contas de teste.
8. Testar recuperação de backup e rollback.
9. Registrar evidências e só então abrir o piloto.

## Não conformidades impeditivas

Segredo exposto, webhook sem assinatura, token em URL, banco sem backup, CORS aberto, Actuator público, migração manual ou decisão automática sem explicação bloqueiam a publicação.

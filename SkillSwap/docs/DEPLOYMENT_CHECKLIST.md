# Checklist de deploy econômico no Railway

## Antes de publicar

- revogar e rotacionar todas as credenciais que apareceram nos arquivos originais ou em mensagens;
- não subir `.env`, `properties.txt`, `Livekit.txt`, chaves privadas nem tokens;
- habilitar autenticação Firebase, domínios autorizados e política de recuperação;
- criar PostgreSQL Railway e executar apenas Flyway;
- configurar `CORS_ALLOWED_ORIGINS` e `APP_FRONTEND_URL` com domínios reais;
- manter integrações opcionais desligadas até que sejam configuradas e testadas;
- decidir se FCM, LiveKit, Redis e RabbitMQ serão usados; recursos não configurados devem ter fallback.

## Plano gratuito ou de baixo custo

O MVP pode usar o plano institucional/gratuito disponível no Railway, Firebase Authentication, FCM, Leaflet/OpenStreetMap ou MapLibre para mapas, Web Speech API, texto/arquivos como fallback de vídeo e Caffeine no lugar de Redis quando a carga permitir. Mapas, vídeo e SMS não devem ser declarados como obrigatórios se não forem configurados; pagamentos estão fora do escopo. SMS foi retirado do caminho principal; o sistema usa e-mail/Firebase e notificações FCM quando habilitadas.

## Sequência

1. Criar banco vazio.
2. Configurar segredos no Railway.
3. Publicar backend e validar `/actuator/health`.
4. Publicar frontend e restringir CORS.
5. Configurar domínio no Firebase.
6. Executar cadastro, login, perfil, troca, chat, Access, notificação e administração.
7. Testar recuperação de backup e rollback.
8. Registrar evidências e só então abrir o piloto.

## Não conformidades impeditivas

Segredo exposto, webhook sem assinatura, token em URL, banco sem backup, CORS aberto, Actuator público, migração manual ou decisão automática sem explicação bloqueiam a publicação.

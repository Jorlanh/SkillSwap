# Arquitetura de software do SkillSwap Access

```text
Usuário comum / pessoa com deficiência / instituição
                 |
         React + TypeScript + Vite
                 |
 Firebase Auth ----+---- FCM opcional
                 |
      Spring Boot — monólito modular
  identidade | perfis | aprendizagem | Access
  comunidades | chat | carteira | pagamentos | admin
                 |
       PostgreSQL + Flyway + Actuator
                 |
       Prometheus ---- Grafana opcional
```

O monólito modular reduz a quantidade de operações no TCC e mantém contratos que podem ser extraídos depois. A API aplica autenticação e autorização no servidor. A persistência é versionada por Flyway; dados demonstrativos são idempotentes.

## Fluxo principal

1. Firebase autentica a pessoa.
2. O frontend obtém um ID token fresco em memória.
3. Axios envia `Authorization: Bearer` para a API.
4. Spring valida assinatura, emissor e audiência; carrega papel persistido.
5. O módulo de perfil fornece habilidades e interesses.
6. O módulo Access compara os dados com oportunidades e devolve pontuação e explicação.
7. A interface apresenta a recomendação com alternativa textual e controles de acessibilidade.

## Comunicação em tempo real

Chat e saldo usam WebSocket sem token em URL. A primeira mensagem contém o ID token; o servidor valida o token e não aceita `senderId` divergente. Mensagens sem identidade são rejeitadas. Em ambiente sem Redis, o fluxo local pode usar processamento síncrono; RabbitMQ e Redis são condicionais.

## Segurança

Segredos ficam no Railway Secret Manager. O navegador não armazena o ID token em localStorage. CORS é restrito, webhook Stripe valida assinatura e idempotência, logs não contêm tokens, e o Actuator deve ser limitado em produção. FCM registra apenas token de dispositivo protegido por sessão e permite revogação.

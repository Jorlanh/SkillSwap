# Runbook de validação antes da produção

## Banco e migrações

1. criar um PostgreSQL vazio no ambiente de homologação;
2. configurar `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` por segredo;
3. iniciar o backend com `spring.jpa.hibernate.ddl-auto=validate`;
4. confirmar que Flyway aplica V1 até V10 sem intervenção manual;
5. verificar índices, constraints de consentimento e tabelas de responsável;
6. testar backup, restauração e exclusão conforme o runbook específico.

## Identidade e e-mail

- confirmar issuer e projeto Firebase do ambiente;
- testar login, logout, token expirado, e-mail não verificado e usuário banido;
- testar SMTP com endereço descartável da equipe;
- testar confirmação de responsável, expiração, reenvio e token reutilizado;
- confirmar que senhas, tokens e conteúdo não aparecem no log.

## Integrações opcionais

- LiveKit: dois navegadores, participante correto, sala inválida, horário fora da janela, câmera/microfone bloqueados e reconexão;
- Calendar: consentimento dos dois participantes, criação, cancelamento e falha do provedor;
- Gemini/Groq: consentimento, timeout, fallback, minimização e indisponibilidade;
- Redis: rate limit distribuído e comportamento quando o serviço fica indisponível;
- Prometheus: `/actuator/health` protegido conforme a política e métricas sem dados pessoais.

## Comandos de validação

No ambiente com dependências disponíveis:

```text
cd backend
./mvnw -q test
./mvnw -q -DskipTests verify

cd ../frontend
npm ci
npm run lint
npm run build
```

Os comandos devem ser executados com variáveis de teste e contas descartáveis. A equipe deve guardar o log, o commit e a data. Não registrar chaves nos artefatos.

## Critério de liberação

Não liberar se houver: migration quebrada, credencial exposta, CORS aberto, Actuator sensível público, histórico sem autorização, vídeo sem vínculo com aula, fluxo de menor sem salvaguarda, backup não restaurável ou teste crítico sem resultado.

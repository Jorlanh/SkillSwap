# Gemini configurável

O SkillSwap usa um modelo Gemini configurável para chat, tradução, simplificação de texto e moderação de conteúdo. Para o chatbot, o backend usa a Interactions API, que recebe `input` e devolve a resposta em `steps`. Como o modelo informado nos logs retornou erro 503, o exemplo usa um modelo Gemini habilitado para a chave, como `gemini-2.5-flash`; confirme no Google AI Studio quais modelos a sua chave pode usar:

```env
GEMINI_MODEL=gemini-2.5-flash
GEMINI_FALLBACK_MODEL=gemini-2.5-flash
GEMINI_API_KEY=coloque-no-secret-manager
GEMINI_TIMEOUT_SECONDS=10
GROQ_API_KEY=coloque-no-secret-manager
GROQ_MODEL=llama-3.1-8b-instant
GROQ_API_URL=https://api.groq.com/openai/v1/chat/completions
```

A chave é enviada pelo backend no cabeçalho `x-goog-api-key`; ela não deve ser colocada no frontend, no Git, em logs ou na query string. O frontend conversa somente com a API Spring Boot. O backend ignora explicitamente o modelo descontinuado `gemini-2.0-flash-001` e repete respostas transitórias `429`, `500`, `502`, `503` e `504` antes de tentar o próximo modelo Gemini. Se todos os modelos Gemini falharem ou expirarem, o chat e a moderação textual usam a API compatível da Groq com `Authorization: Bearer`.

O tempo máximo da chamada Gemini/Groq fica limitado por `GEMINI_TIMEOUT_SECONDS`, evitando que o chatbot ou a moderação fiquem presos por dezenas de segundos. A troca de provedor é registrada apenas pelo nome do provedor e do modelo, nunca pela chave.

## Conta suspensa e contestação

Durante uma suspensão geral, o filtro de segurança bloqueia as operações da plataforma, mas permite consultar `GET /api/account/suspension/status`, `GET /api/home/notifications`, marcar notificações como lidas e enviar `POST /api/account/suspension/appeal`. A contestação é encaminhada pelo backend ao endereço configurado em `SUPPORT_EMAIL` (por padrão, `suporte.skillswap@gmail.com`) usando as credenciais SMTP do Railway. O frontend apresenta o motivo, o prazo e o formulário de contestação em `/suspensao`.

Configure também:

```env
SUPPORT_EMAIL=suporte.skillswap@gmail.com
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=conta-remetente
MAIL_PASSWORD=senha-de-app-do-Gmail
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
```

O usuário não recebe acesso ao feed, chat, publicação, comunidades ou outras operações enquanto a suspensão estiver ativa. A decisão de punição continua sendo registrada pelo backend; indisponibilidade dos provedores de IA não aplica banimento permanente automaticamente.

## Imagens

O recurso de imagem de conquistas permanece separado, usando o endpoint/modelo de imagem configurado no serviço de conquistas. O modelo Flash de texto não deve ser tratado como gerador de imagens. Se a equipe não quiser manter esse recurso, desative-o e preserve o fallback textual.

## Produção

Crie a chave no Google AI Studio, restrinja o projeto e os limites de uso, configure `GEMINI_API_KEY` como segredo no Railway e monitore custos, latência, quotas e conteúdo enviado. A API deve receber somente o contexto mínimo necessário e não deve enviar senhas, tokens, dados sensíveis ou conversas completas sem finalidade definida.

Fonte oficial dos modelos: https://ai.google.dev/gemini-api/docs/models

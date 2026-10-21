# Gemini 2.5 Flash

O SkillSwap Access usa `gemini-2.5-flash` como modelo padrão para chat, tradução, simplificação de texto e moderação de conteúdo. O identificador fica configurável em `GEMINI_MODEL`, mas o valor recomendado para esta entrega é:

```env
GEMINI_MODEL=gemini-2.5-flash
GEMINI_API_KEY=coloque-no-secret-manager
```

A chave é enviada pelo backend no cabeçalho `x-goog-api-key`; ela não deve ser colocada no frontend, no Git, em logs ou na query string. O frontend conversa somente com a API Spring Boot.

## Imagens

O recurso de imagem de conquistas permanece separado, usando o endpoint/modelo de imagem configurado no serviço de conquistas. O Gemini 2.5 Flash não deve ser tratado como gerador de imagens. Se a equipe não quiser manter esse recurso, desative-o e preserve o fallback textual.

## Produção

Crie a chave no Google AI Studio, restrinja o projeto e os limites de uso, configure `GEMINI_API_KEY` como segredo no Railway e monitore custos, latência, quotas e conteúdo enviado. A API deve receber somente o contexto mínimo necessário e não deve enviar senhas, tokens, dados sensíveis ou conversas completas sem finalidade definida.

Fonte oficial do identificador: https://ai.google.dev/gemini-api/docs/models/gemini-2.5-flash

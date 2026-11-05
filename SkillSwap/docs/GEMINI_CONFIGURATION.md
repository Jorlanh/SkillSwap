# Gemini configurável

O SkillSwap usa um modelo Gemini configurável para chat, tradução, simplificação de texto e moderação de conteúdo. Para a chave usada nesta implantação, o modelo recomendado é `gemini-3.8-flash`. O backend tenta também o modelo de fallback e consulta os modelos habilitados para a chave quando necessário:

```env
GEMINI_MODEL=gemini-3.8-flash
GEMINI_FALLBACK_MODEL=gemini-2.0-flash-001
GEMINI_API_KEY=coloque-no-secret-manager
```

A chave é enviada pelo backend no cabeçalho `x-goog-api-key`; ela não deve ser colocada no frontend, no Git, em logs ou na query string. O frontend conversa somente com a API Spring Boot.

## Imagens

O recurso de imagem de conquistas permanece separado, usando o endpoint/modelo de imagem configurado no serviço de conquistas. O modelo Flash de texto não deve ser tratado como gerador de imagens. Se a equipe não quiser manter esse recurso, desative-o e preserve o fallback textual.

## Produção

Crie a chave no Google AI Studio, restrinja o projeto e os limites de uso, configure `GEMINI_API_KEY` como segredo no Railway e monitore custos, latência, quotas e conteúdo enviado. A API deve receber somente o contexto mínimo necessário e não deve enviar senhas, tokens, dados sensíveis ou conversas completas sem finalidade definida.

Fonte oficial dos modelos: https://ai.google.dev/gemini-api/docs/models

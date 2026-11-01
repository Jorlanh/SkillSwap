# Adendo de implementação — SkillSwap Access

Escopo final exclusivamente de software. A equipe atende usuários comuns e prioriza acessibilidade para pessoas com deficiência. O núcleo implementado usa React, Spring Boot, PostgreSQL, Flyway, Firebase e recomendações explicáveis. Integrações como FCM, vídeo, mapas, Redis, RabbitMQ e Prometheus são ativadas apenas com configuração e testes; cada uma tem fallback ou é classificada como condicional.

O hardware foi removido do TCC. Não há protótipo, diagrama ou requisito físico nesta versão.

## IA do produto

O chatbot, a tradução, a simplificação de texto e a moderação usam Gemini 2.5 Flash, configurado por `GEMINI_MODEL=gemini-2.5-flash`. A chave fica somente no backend/Railway. A geração de imagens de conquistas é uma integração separada, pois o Gemini 2.5 Flash não é modelo de geração de imagens.


## Atualização da versão gratuita

Nesta versão, a equipe retirou a cobrança do fluxo de usuários. Stripe, SwapCoins, premium, gorjetas e limites pagos não fazem parte da entrega e suas rotas são bloqueadas. Após o cadastro, a pessoa escolhe preferências de acessibilidade sem informar ou receber uma classificação de deficiência. O alto contraste e o tamanho do texto são aplicados no navegador. A aula exige confirmação do aluno e do professor; depois disso, o aluno pode gerar um comprovante acessível de conclusão.

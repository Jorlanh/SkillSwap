# Recursos anteriormente chamados de premium

## Estado desta entrega

O SkillSwap é gratuito para os usuários. `APP_COMMERCE_ENABLED=false` é o padrão e faz com que os recursos abaixo não debitem SwapCoin nem exijam checkout. Este arquivo permanece como referência de compatibilidade do código legado e de possível evolução, não como uma cobrança ativa.

## Cotas técnicas da IA

Cada usuário recebe 20 consultas técnicas do chatbot ELOS por mês. A anotação `@PremiumFeature` aciona um aspecto AOP que registra o consumo em `premium_ai_usage`. No modo gratuito, ao exceder a cota, a plataforma continua sem cobrança; a telemetria serve apenas para acompanhar limites operacionais. O modo de cobrança legado só existe se for habilitado separadamente e revisado pela equipe.

Análise de perfil e plano de estudos continuam disponíveis como recurso de IA, sem cobrança no modo gratuito (`POST /api/premium/ai/profile-plan`).

## Coin sinks

| Recurso | Custo no modo gratuito | Benefício |
|---|---:|---|
| Impulsionar proposta | 0 SC | 48 horas de maior exposição |
| Destacar post | 0 SC | 48 horas em destaque |
| Slot de portfólio | 0 SC | Aumenta a capacidade do perfil |
| Hora extra de aula | 0 SC | Registra a extensão da aula sem cobrança |
| Patrocínio Karma de proposta/post | 0 SC | Dá 48 horas de visibilidade a outro usuário ou projeto |

As rotas autenticadas são `GET /api/premium/catalog`, `GET /api/premium/usage/ai`, `POST /api/premium/ai/profile-plan`, `POST /api/premium/proposals/{id}/boost`, `POST /api/premium/proposals/{id}/sponsor`, `POST /api/premium/posts/{id}/highlight`, `POST /api/premium/posts/{id}/sponsor`, `POST /api/premium/profile/portfolio-slots` e `POST /api/premium/lessons/{id}/extra-hours`.

## Conquistas

Ao desbloquear uma conquista, `AchievementServiceImpl` cria um lançamento idempotente `ACHIEVEMENT_REWARD`. A conquista de 10 trocas concede 25 SC; novas conquistas podem ser adicionadas no mesmo serviço sem duplicar recompensas.

## Limites de texto, gorjetas e Karma

No modo gratuito, os endpoints `POST /api/premium/profile/bio-limit` e `POST /api/premium/profile/post-limit` não debitam o ledger. Ainda existem limites técnicos de tamanho para proteger o serviço, mas não há recurso bloqueado por pagamento.

Transferências livres continuam disponíveis em `/api/wallet/transfers`; a rota `/api/wallet/tips` usa apenas pontos internos de participação. SwapCoin não representa dinheiro, não pode ser comprado no modo gratuito e não é necessário para usar a plataforma.

A primeira avaliacao de 5 estrelas recebida por uma pessoa em uma troca concluida concede 5 SC uma unica vez. O sistema Karma e separado do saldo: cada patrocinio gera pontos para quem apoia e para o autor, registra eventos em `karma_events` e expoe `/api/karma/me` e `/api/karma/top`.

## RabbitMQ e WebSocket

O código de webhook Stripe permanece como extensão desativada no modo gratuito. A plataforma não cria checkout nem cobra usuários quando `APP_COMMERCE_ENABLED=false`; qualquer futura ativação exige decisão separada, revisão jurídica e nova homologação.

O `WalletBalanceWebSocketHandler` autentica o ID token Firebase no handshake e publica `wallet.balance.updated` após cada compra, transferência, recompensa, sink ou aula extra. Em produção, use WSS/TLS e não registre tokens na URL. Para desenvolvimento sem broker, defina `RABBITMQ_ENABLED=false` e o webhook usa o fallback síncrono idempotente.

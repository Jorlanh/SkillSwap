# Prometheus e observabilidade

Prometheus é o componente que coleta séries temporais de métricas técnicas da aplicação. Ele não é banco de usuários, não substitui logs e não deve receber texto de chat, token, e-mail ou outro dado pessoal desnecessário. No SkillSwap Access, o Spring Boot Actuator pode expor `/actuator/prometheus` em rede protegida.

## O que medir

- latência e taxa de erro por rota;
- disponibilidade e tempo de resposta do health check;
- conexões WebSocket abertas e falhas de autenticação;
- tempo de consulta de recomendações;
- pool de conexões PostgreSQL;
- filas e retries, somente se RabbitMQ for habilitado;
- uso de memória e CPU.

## Grafana e alertas

Grafana é opcional e apenas visualiza as métricas. Alertas iniciais: erro 5xx sustentado, latência p95 alta, banco indisponível, falha de migration e aumento de rejeições de WebSocket. No Railway, o plano econômico pode começar com métricas nativas e um Prometheus externo gratuito; a decisão depende de limites atuais e deve ser conferida na implantação.

## Implementação segura

Restringir o endpoint, aplicar autenticação de infraestrutura quando possível, não usar dados pessoais em labels, limitar retenção e documentar o custo. A equipe deve validar a coleta em homologação antes de declarar observabilidade pronta.

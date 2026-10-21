# Segurança: rotação obrigatória antes do deploy

Os arquivos originais continham configurações sensíveis. Mesmo que fossem de teste, devem ser tratados como comprometidos porque foram compartilhados. Antes de publicar:

1. revogue e gere novamente chaves Stripe, LiveKit, Firebase Admin, e-mail, Cloudinary, banco e qualquer serviço de IA;
2. altere a senha PostgreSQL e remova segredos do histórico Git;
3. substitua arquivos reais por `.env.example` com placeholders;
4. configure segredos no Railway, nunca no frontend além das configurações públicas do Firebase;
5. revise logs, screenshots e artefatos para impedir vazamento;
6. faça teste de autenticação, autorização, webhook e exclusão de dados.

Não há segurança de ponta apenas por usar Firebase. Segurança é um processo de configuração, menor privilégio, atualização, backup, observabilidade, resposta a incidentes e revisão humana.

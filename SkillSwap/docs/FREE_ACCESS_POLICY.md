# Política técnica da versão gratuita

A SkillSwap é gratuita para os usuários nesta versão. Não existe cobrança obrigatória para cadastro, troca de habilidades, conversa, agendamento, aula ou geração do comprovante de conclusão.

## Bloqueios aplicados

As rotas `/api/wallet/**`, `/api/premium/**` e `/api/payments/**` são rejeitadas pela configuração de segurança. A tela Financeiro foi removida da navegação e o chatbot não desconta créditos.

Stripe, SwapCoins, compras, gorjetas, destaque pago, patrocínio, limite pago de perfil e horas extras pagas não participam do fluxo da aplicação.

O código legado dessas integrações permanece isolado para fins de histórico e poderá ser removido em uma limpeza futura. A presença de classes antigas não significa que a funcionalidade esteja disponível.

## Acessibilidade

As preferências são informadas pela própria pessoa e podem ser alteradas ou apagadas. O sistema não tenta descobrir ou mostrar publicamente uma deficiência a partir de cor, forma ou respostas. Marcadores visuais são apenas temas de interface.

O contraste intenso usa fundo escuro, texto claro, foco visível e destaque não dependente somente de cor. Legendas e VLibras são recursos complementares e dependem do ambiente; a documentação não declara certificação WCAG sem auditoria independente.

## Comprovante de aula

A aula passa a ser concluída quando aluno e professor confirmam a participação. O certificado apresenta nome do aluno, aula, duração, professor, data e identificador. Ele é um comprovante de participação e não substitui diploma ou certificação profissional.

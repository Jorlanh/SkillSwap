# Plano de resposta a incidentes

Solicitações de confirmação de responsável também são dados pessoais. Em caso de exposição de token, revogar as solicitações afetadas, invalidar os links pendentes, gerar novos tokens somente após nova solicitação e avaliar a comunicação aos titulares e à autoridade competente.

1. **Identificar:** registrar data, sistema, tipo de evento e responsável pelo primeiro atendimento.
2. **Conter:** revogar tokens, desligar credenciais afetadas, bloquear acessos e preservar evidências sem copiar dados desnecessários.
3. **Avaliar:** identificar titulares, categorias de dados, sensibilidade, escala, países e risco de dano.
4. **Mitigar:** corrigir a falha, forçar troca de credenciais, remover dados expostos e comunicar os titulares de forma simples.
5. **Comunicar:** avaliar comunicação à ANPD e aos titulares dentro do prazo aplicável. A ANPD informa que incidentes com risco ou dano relevante devem ser comunicados pelo controlador.
6. **Aprender:** registrar causa, impacto, correção, teste de retorno e mudança preventiva.

## Dados mínimos do registro interno

Manter identificador do incidente, horário de descoberta, sistemas afetados, categorias de dados, medidas adotadas, decisão de comunicação, responsáveis e data de encerramento. Não colocar conteúdo de mensagens, tokens ou senhas no log do incidente.

## Contatos a preencher

- responsável técnico: definir;
- encarregado/contato de privacidade: definir;
- contato da hospedagem: registrar no inventário;
- canal para titulares: definir;
- canal de comunicação com a ANPD: seguir o procedimento oficial vigente.

## Escalonamento para incidentes envolvendo menores

Se houver suspeita de abuso, aliciamento, exposição de imagem íntima, ameaça ou contato indevido envolvendo menor, conter imediatamente a interação, preservar somente os metadados necessários, bloquear a conta conforme a política da equipe, acionar o controlador e o encarregado/DPO e seguir orientação jurídica e das autoridades competentes. Não enviar o conteúdo a ferramentas de IA para “investigar” o caso.

## Simulação obrigatória

Antes do piloto, realizar um exercício com dados sintéticos: vazamento de token de responsável, acesso indevido ao histórico de chat e exposição de credencial de fornecedor. Registrar tempo de detecção, contenção, decisão de comunicação, recuperação e ações preventivas.

## Medidas específicas do release

- revogar imediatamente chaves de Firebase, IA, LiveKit, e-mail, mídia e banco quando houver suspeita de exposição;
- bloquear a funcionalidade opcional afetada sem desligar o núcleo gratuito da plataforma, quando isso for possível;
- consultar os logs sem registrar tokens, senhas, conteúdo de mensagens ou dados de deficiência;
- documentar se houve transferência internacional e quais operadores foram envolvidos.

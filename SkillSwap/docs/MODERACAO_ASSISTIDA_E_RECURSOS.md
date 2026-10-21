# Moderação assistida, decisões humanas e recursos

## Política adotada

A SkillSwap separa triagem automática de punição de conta. A IA Gemini 2.5 Flash classifica textos de chat, posts, comentários e denúncias. Quando identifica risco ou não consegue concluir a análise, o conteúdo pode ser bloqueado ou colocado em contenção temporária. Essa classificação não é um banimento e não substitui a revisão administrativa.

O fluxo de um caso é:

1. a IA classifica categoria, confiança e recomendação;
2. o conteúdo é bloqueado ou fica em contenção;
3. a denúncia entra na fila de moderação;
4. um administrador analisa o conteúdo original, o motivo da denúncia e a evidência;
5. o administrador pode liberar, advertir, suspender por dias, suspender por meses ou banir permanentemente;
6. a decisão registra ação, motivo, evidência, responsável e data;
7. a pessoa afetada pode apresentar recurso, que retorna para a fila.

Quando um chat, post ou comentário é bloqueado pela triagem, o sistema também registra um caso de origem `AI_CONTENT_BLOCK` com o conteúdo contido. Assim, o bloqueio não desaparece sem rastreabilidade. Uma denúncia de usuário continua identificada como `USER_REPORT`.

## Suspensão temporária automática opcional

Por padrão, a opção `Contenção temporária automática de conta` fica desligada. Nesse modo, a IA apenas classifica e contém conteúdo; nenhuma suspensão ou banimento de conta é aplicado automaticamente.

O painel possui um controle explícito para ativar a contenção temporária de emergência. Quando ativada, ela só pode aplicar uma suspensão temporária para contas não administrativas quando a classificação for `BLOCK` e a confiança for de pelo menos 0,85. A duração padrão é de 24 horas, configurável entre 1 e 168 horas. O caso continua na fila e precisa de decisão humana posterior. A IA nunca aplica advertência, suspensão por meses ou banimento permanente.

Contas com papel `ADMIN` não recebem essa contenção automática. Uma eventual decisão sobre administrador precisa ser feita por pessoa administradora, com confirmação humana explícita, e fica registrada no caso.

## Indisponibilidade da IA

Se o provedor Gemini estiver indisponível ou sem chave configurada, o conteúdo novo é colocado em contenção e a triagem recebe a categoria `MODERATION_UNAVAILABLE`. O sistema não converte indisponibilidade em banimento automático. O administrador deve revisar o caso antes de qualquer sanção.

## Evidência e recurso

A denúncia aceita o texto original denunciado em `reportedContent`; quando esse campo não está disponível, o motivo da denúncia é usado apenas como sinal de triagem. A decisão humana possui justificativa e evidência separadas. A pessoa afetada pode chamar `POST /api/reports/{reportId}/appeal` com a justificativa do recurso. O caso passa para `APPEALED` e volta a aparecer na fila administrativa.

## Endpoints administrativos

- `GET /api/console-management/moderation/cases`: fila de casos pendentes, em análise ou com recurso;
- `GET /api/console-management/moderation/policy`: política atual;
- `PUT /api/console-management/moderation/policy`: liga ou desliga a contenção temporária automática;
- `POST /api/console-management/moderation/cases/{id}/decision`: registra a decisão humana;
- `POST /api/reports/{id}/appeal`: registra recurso da pessoa afetada.

As alterações de banco estão em `V11__assisted_moderation_queue.sql`. O painel administrativo apresenta a fila, a classificação da IA, a indicação de contenção automática e o estado de recurso.

## Limites declarados

A implementação classifica texto. Ela não interpreta automaticamente vídeo, áudio ou transmissão de tela. Esses conteúdos continuam sujeitos a denúncia, bloqueio pelos controles da chamada e revisão humana conforme a evidência apresentada.

# Política de retenção e exclusão

Os prazos abaixo são uma proposta inicial e precisam ser confirmados pela equipe com o responsável jurídico ou encarregado.

| Dado | Regra proposta |
|---|---|
| Conta ativa | enquanto a pessoa usar o serviço ou houver base legal válida |
| Consentimentos | durante a conta e pelo período necessário para demonstrar a decisão |
| Token de notificação | até a revogação, expiração ou 180 dias sem uso |
| Logs de acesso | prazo curto, preferencialmente até 180 dias, salvo incidente ou obrigação |
| Mensagens e arquivos | enquanto forem necessários à conversa, com exclusão quando solicitada e possível |
| Aulas e certificados | enquanto forem necessários ao histórico solicitado pelo participante |
| Incidentes | conforme o procedimento interno e o período legal aplicável |
| Backups | com prazo definido pelo provedor e exclusão no próximo ciclo tecnicamente possível |
| Solicitações de responsável | token somente em hash; expira em 72 horas; manter o status pelo período mínimo necessário para governança e atendimento |

A exclusão da conta usa anonimização local para preservar somente o que for indispensável à segurança, à prevenção de fraude ou a uma obrigação legal. A equipe deve testar a remoção em PostgreSQL, Firebase, Cloudinary, Calendar, LiveKit, e-mail, caches e backups quando esses serviços estiverem ativos.

No código, `DELETE /api/privacy/me` anonimiza a conta local, limpa campos identificáveis e marca `deleted_at`. O frontend solicita também a exclusão do usuário atual no Firebase. Como a exclusão de cada fornecedor depende de suas APIs e contratos, o procedimento deve ser conferido depois de cada integração ser ativada.

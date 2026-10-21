# Inventário de dados e fornecedores

Este inventário deve ser atualizado sempre que uma integração for ativada. “Gratuito” não significa “fora do escopo da proteção de dados”.

| Grupo | Dados | Finalidade | Fornecedor possível | Decisão antes da produção |
|---|---|---|---|---|
| Conta | e-mail, nome, identificador Firebase | autenticação e conta local | Firebase | conferir DPA, retenção e região |
| Perfil | nome, habilidades, bio, localização | troca e descoberta | PostgreSQL/Railway | limitar campos públicos |
| Comunicação | mensagens e arquivos | conversa entre usuários | PostgreSQL/Cloudinary | definir retenção e exclusão |
| Aula | participantes, horário, título, certificado | organização e comprovação | PostgreSQL/Google Calendar | usar somente com finalidade informada |
| Vídeo | identidade de sala e mídia em trânsito | aula remota | LiveKit | confirmar retenção, gravação e região |
| IA | texto enviado ao assistente ou moderação | simplificação e segurança | Gemini/Groq | minimizar conteúdo e oferecer desativação |
| Notificações | token FCM | avisos | Firebase Cloud Messaging | consentimento opcional e revogação |
| Segurança | IP e horário de acesso | prevenção e auditoria | aplicação/Railway | prazo curto e anonimização |
| Responsável de menor | nome, e-mail, vínculo, status, datas, hash do token | confirmação de responsável e governança | PostgreSQL e serviço de e-mail | token em hash; expiração em 72 horas; definir retenção final |

Para cada fornecedor, registrar: controlador ou operador, finalidade, categorias de dados, país de processamento, subprocessadores, retenção, criptografia, contrato, mecanismo de transferência internacional e procedimento de exclusão.

## Dados relacionados a acessibilidade

Preferências como alto contraste, avatar de Libras, tema e marcador de cor/forma são tratadas como configurações escolhidas pela pessoa. O marcador visual não é diagnóstico, não é obrigatório e não é publicado no perfil por padrão. O backend só devolve esse marcador em um perfil público quando `accessibility_visual_marker_public` está habilitado pelo próprio titular. A equipe não deve inferir deficiência, saúde ou renda a partir dessas preferências.

## Política de ativação sem cobrança ao usuário

O SkillSwap pode continuar gratuito para os usuários. O custo do fornecedor não muda a obrigação de informar o tratamento. Integrações que não forem necessárias ao MVP devem permanecer desativadas; quando ativadas, devem aparecer na política, no consentimento e no inventário.

O código entregue usa `APP_COMMERCE_ENABLED=false` como padrão. Nesse modo, recursos anteriormente classificados como premium não debitam SwapCoin e não exigem checkout do usuário. A equipe deve manter essa configuração no Railway enquanto a decisão for oferecer a plataforma gratuitamente.

O link enviado ao responsável contém um token aleatório de uso único no fragmento da URL. O banco armazena somente seu hash; a aplicação não guarda o link em texto puro e o fragmento não é enviado ao servidor web. A confirmação verifica o controle do e-mail informado, mas a equipe não deve apresentar esse mecanismo como prova jurídica universal de tutela ou autoridade parental.

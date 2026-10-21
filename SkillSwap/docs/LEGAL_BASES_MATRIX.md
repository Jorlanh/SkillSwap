# Matriz de finalidades e bases legais

Esta matriz é uma proposta técnica para orientar a revisão do controlador e de um profissional jurídico. Ela não substitui a análise jurídica da LGPD, do GDPR, das regras locais de menores ou dos contratos dos fornecedores.

| Tratamento | Dados principais | Base a avaliar | Necessidade e limite | Controle no produto |
|---|---|---|---|---|
| Criar e manter conta | e-mail, identificador, nome e credenciais do provedor | execução de contrato ou procedimentos pré-contratuais | manter somente o necessário para a conta | Firebase + conta local |
| Autenticar e proteger a sessão | identificador Firebase, claims, logs técnicos | execução de contrato, segurança e obrigação legal, conforme o caso | não usar token em URL; limitar logs | JWT, filtro de banimento e logs mínimos |
| Perfil e troca de habilidades | nome público, habilidades e preferências escolhidas | execução de contrato; consentimento quando o campo for opcional | não inferir deficiência, renda ou diagnóstico | DTO público, privacidade do perfil |
| Chat e aula | participantes, mensagens, arquivos e horários | execução de contrato; segurança e prevenção de abuso | acesso somente às partes autorizadas | autorização de conversa e aula |
| Vídeo LiveKit | identidade da sala e metadados técnicos | consentimento opcional e execução da aula | sala vinculada à aula e janela de horário | token LiveKit restrito |
| Google Calendar | e-mails dos participantes, horário e título | consentimento opcional | não criar evento sem consentimento dos dois participantes | integração desligada por padrão |
| IA Gemini/Groq | texto escolhido pelo usuário ou conteúdo de moderação | consentimento opcional; segurança pode exigir tratamento mínimo | minimizar, não enviar diagnóstico ou dados desnecessários | `OPTIONAL_AI`, provedor híbrido |
| Notificações | token FCM e preferências | consentimento opcional | revogável e desativável | `OPTIONAL_NOTIFICATIONS` |
| Preferências de acessibilidade | tema, contraste, Libras, marcador visual | escolha explícita do titular; consentimento quando aplicável | não tratar preferência como diagnóstico | configuração local e opt-in público |
| Dados de menores | faixa etária, dados do responsável e status | melhor interesse; consentimento do responsável quando exigido; outras bases apenas após revisão | não impedir o uso geral, mas restringir contato adulto–menor não vinculado | token de uso único, status e salvaguarda |
| Direitos do titular | pedidos de exportação, correção, revogação e exclusão | obrigação legal | manter evidência mínima do atendimento | Centro de Privacidade |
| Segurança e incidentes | IP, horário, identificadores de evento | segurança, obrigação legal ou legítimo interesse, conforme a jurisdição | prazo definido e acesso restrito | logs e runbook |

## Dados sensíveis e acessibilidade

O sistema não deve solicitar diagnóstico médico para entregar a experiência acessível. Preferências de interface e recursos escolhidos pelo usuário não devem ser usados para inferir deficiência. Se a equipe decidir coletar informação de saúde, deficiência, biometria ou outra categoria sensível, deverá criar uma finalidade separada, justificar a base legal específica, limitar o acesso e atualizar o RIPD antes de habilitar o campo.

## Decisão obrigatória antes da publicação

Para cada linha, o controlador deve confirmar: finalidade, base, necessidade, prazo, destinatários, país de processamento e forma de exercício dos direitos. A matriz aprovada deve ser versionada junto da política publicada.

# Adendo — privacidade e proteção de dados

O SkillSwap foi ajustado para continuar gratuito para os usuários e para oferecer um Centro de Privacidade. A implementação registra consentimentos por finalidade e versão, permite exportar dados, corrigir informações, revogar integrações opcionais e solicitar a desativação da conta. A API também impede a exposição direta da entidade de usuário em perfil público e restringe alterações de conta à própria pessoa ou ao administrador. O marcador visual de acessibilidade é privado por padrão e só pode ser exposto por escolha explícita do titular.

O modo gratuito é controlado por `APP_COMMERCE_ENABLED=false`. Assim, o uso normal da troca de habilidades, aulas, chat, certificados e recursos de acessibilidade não exige SwapCoin nem checkout. Essa decisão não impede que a equipe tenha custos operacionais com hospedagem ou fornecedores, mas não cria cobrança para os usuários.

Pessoas menores de 18 anos podem usar a SkillSwap. O cadastro solicita nome, e-mail e vínculo de um responsável, cria uma solicitação com status `PENDING_GUARDIAN` e envia um link de uso único com validade de 72 horas. A conta continua acessível enquanto a confirmação estiver pendente. O banco armazena somente o hash do token e registra a confirmação em `ss_guardian_consents` e `ss_privacy_consents`.

Essa confirmação valida o controle do endereço de e-mail informado, mas não prova sozinha identidade ou autoridade legal. A equipe deve revisar os requisitos de cada país e aplicar salvaguardas adicionais para faixas etárias mais baixas antes da publicação comercial.

A equipe não afirma que o sistema possui certificação LGPD ou GDPR. A conformidade depende da publicação da política, da identificação do controlador, da avaliação dos fornecedores, da definição dos prazos de retenção, dos contratos de transferência internacional, dos testes de segurança e da revisão jurídica ou do encarregado.

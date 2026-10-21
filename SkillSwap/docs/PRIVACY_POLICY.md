# Política de Privacidade do SkillSwap

Versão: `2026-10-21`  
Idiomas: português brasileiro e inglês

> Este documento é uma base técnica para publicação. Antes de colocar o produto em operação comercial, a equipe precisa preencher a identidade do controlador, o contato de privacidade, os fornecedores efetivamente usados e os prazos finais de retenção.

## 1. Controlador e contato

Controlador: **SkillSwap — definir razão social ou responsáveis antes da publicação**.  
Contato de privacidade/encarregado: **definir e-mail oficial antes da publicação**.

Esta política é apresentada na aplicação em português e inglês. A identidade do controlador e o canal de atendimento continuam como campos que precisam ser preenchidos pela equipe.

## 2. Dados tratados

O sistema pode tratar nome, nome de usuário, e-mail, telefone, data de nascimento, cidade, estado, país, biografia, habilidades, publicações, mensagens, aulas, certificados, preferências de acessibilidade, registros técnicos e tokens de notificação.

O marcador visual é uma preferência escolhida pela pessoa. Ele não é diagnóstico e não deve ser usado para concluir que alguém tem ou não tem deficiência. A publicação do marcador é opcional e fica desativada por padrão; só aparece no perfil público quando a própria pessoa habilita essa opção. Quando o perfil é privado, a API pública não expõe biografia, contatos ou habilidades.

## 3. Finalidades

- autenticar a conta e impedir fraude;
- manter o perfil e a troca de habilidades;
- organizar aulas e emitir certificados solicitados;
- permitir comunicação entre participantes;
- proteger a comunidade e investigar abusos;
- cumprir solicitações de acesso, correção e exclusão;
- executar integrações que a pessoa ativar de maneira clara.

O SkillSwap não vende dados pessoais e não deve usar informações de deficiência, saúde ou renda para discriminar pessoas.

## 4. Consentimentos

Os consentimentos necessários são registrados por finalidade, versão, idioma, origem, data e estado. Consentimentos opcionais para IA, calendário, vídeo e notificações podem ser revogados no Centro de Privacidade.

O consentimento não deve ser usado para justificar tudo: a equipe deve registrar também as bases legais escolhidas para autenticação, contrato, segurança, cumprimento de obrigação e demais finalidades.

## 5. Fornecedores e transferências

Firebase, Railway, PostgreSQL gerenciado, Google Calendar, Gemini, Groq, LiveKit, Cloudinary, Twilio e serviços de e-mail só podem ser ativados depois de registrados no inventário de dados e avaliados quanto a contrato, localização, subprocessadores, retenção e transferência internacional.

## 6. Direitos do titular

O Centro de Privacidade oferece exportação, correção, revogação de integrações opcionais e desativação da conta. A exportação não inclui segredos de autenticação. A conta é anonimizada localmente e o cliente tenta remover também a identidade do Firebase.

Dados necessários para segurança, prevenção de fraude, auditoria ou obrigação legal podem ser preservados de forma limitada e, quando possível, anonimizados.

## 7. Menores

Pessoas menores de 18 anos podem utilizar a plataforma. O cadastro solicita nome, e-mail e vínculo de um responsável. O responsável recebe um link de uso único, com validade de 72 horas, e a conta permanece acessível com o status `PENDING_GUARDIAN` enquanto a confirmação não for registrada.

A aplicação armazena o hash do token, o estado da solicitação e os dados mínimos do responsável. O e-mail confirma o controle do endereço informado, mas não prova sozinho identidade ou autoridade legal. A equipe deve revisar requisitos locais, especialmente para faixas etárias mais baixas, antes de publicar o serviço em cada país.

## 8. Plataforma gratuita

O uso da plataforma pelos usuários é gratuito. O código não debita SwapCoin nem exige pagamento para usar a troca de habilidades, aulas, chat, certificados ou recursos de acessibilidade quando as integrações estiverem habilitadas. Serviços externos podem ter limites técnicos ou custos de operação para a equipe, mas esses custos não são repassados aos usuários nesta versão.

## 9. Segurança e incidentes

O acesso à API usa tokens Firebase, as credenciais de serviços ficam em variáveis de ambiente e os DTOs não devem expor senhas, tokens ou segredos. Incidentes devem ser registrados, avaliados e comunicados conforme a legislação aplicável e as orientações da ANPD.

## 10. English summary

SkillSwap processes only the data needed for authentication, profiles, skill exchange, lessons, community safety and user requests. It does not sell personal data. Optional AI, calendar, video, notification and storage providers must be enabled for a clear purpose and can be revoked through the Privacy Center. Users can access, correct, export or request account deactivation. Visual accessibility markers are private by default and are displayed publicly only after the user enables that option. People under 18 can use the platform through a guardian confirmation flow; pending accounts remain accessible. Email confirmation alone does not replace local legal requirements. The platform is free for users in this release.

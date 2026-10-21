# Status de validação da entrega

## Verificações estáticas realizadas

- revisão dos contratos Java/TypeScript, das migrações V9/V10 e das rotas de privacidade;
- conferência de que o modo gratuito usa `APP_COMMERCE_ENABLED=false` e não debita SwapCoin;
- conferência de que o `.env` e credenciais de provedores não entram na entrega.

## Escopo institucional implementado

- `V7__institutional_workspace.sql` cria organizacoes, memberships e vincula oportunidades a organizacoes.
- `InstitutionalController` expoe organizacoes, membros, oportunidades, candidaturas, dashboard e assinatura.
- `InstitutionalWorkspacePage` fornece o painel para criacao de organizacao, assentos, equipe, funil e checkout.
- O checkout Stripe existe como extensão institucional, mas fica desativado quando `APP_COMMERCE_ENABLED=false`, que é o padrão desta entrega gratuita.

## Validação de build e execução

Nesta sessão, a compilação Maven não foi confirmada porque o Maven Wrapper não conseguiu criar/acessar o diretório local de distribuição. O frontend também está sem `node_modules`. Não apresentamos build, testes de integração ou homologação de provedores como concluídos.

## Limitações do ambiente desta sessão

O `vite build`, a verificação TypeScript e o lint não foram executados nesta sessão porque as dependências do frontend (`node_modules`) não estão presentes. Em uma máquina de desenvolvimento normal, execute `npm ci`, `npm run lint` e `npm run build` dentro de `frontend`.

Os testes Maven legados também não chegaram à execução por causa do bloqueio de acesso ao diretório local do Maven. Isso foi mantido explícito para não apresentar validação de testes como se tivesse ocorrido.

## Homologação necessária

Para validar runtime, configure PostgreSQL, Firebase Authentication e as variáveis privadas de `backend/src/main/resources/application.properties` via ambiente. Flyway deve aplicar V1 a V10; não use `ddl-auto=update`. A V8 adiciona a preferência visual opt-in e os campos usados para conclusão e certificado de aula. A V9 adiciona o Centro de Privacidade, consentimentos versionados e `deleted_at`. A V10 adiciona faixa etária, status de confirmação e solicitações de responsável com token armazenado somente em hash.

## Alterações desta entrega

As alterações de alto contraste, marcador visual e certificado foram conferidas por inspeção estática dos contratos Java, TypeScript, migration e documentação. A compilação completa destas alterações não foi executada nesta sessão porque o Maven precisou acessar o repositório externo e a rede do ambiente estava bloqueada; o frontend também foi entregue sem `node_modules`. Execute `backend/mvnw.cmd -q -DskipTests compile` e, em `frontend`, `npm ci`, `npm run lint` e `npm run build` em uma máquina com dependências disponíveis antes do deploy.

## Privacidade adicionada nesta revisão

- `PrivacyController` implementa política pública, cadastro de menores com responsável, consentimentos, revogação, exportação, correção e desativação da conta.
- `ProfileController` não retorna mais a entidade `User` em perfil público.
- `UserController` restringe atualização à própria conta ou a `ADMIN`, recebe `UserUpdateRequest` e evita mass assignment administrativo para contas comuns.
- O cadastro React exige data de nascimento e aceite da política/termos para novas contas; para menores, solicita responsável e mantém o acesso durante a pendência.
- A revisão jurídica continua pendente e não deve ser apresentada como certificação LGPD/GDPR.
- `V9__privacy_center_and_data_rights.sql` adiciona consentimentos versionados, `deleted_at` e publicação opt-in do marcador visual.
- `V10__minor_guardian_validation.sql` adiciona a validação do responsável e a nova tela pública de confirmação.
- O Google Calendar não impede a inicialização quando está desativado ou sem credencial.
- `APP_COMMERCE_ENABLED=false` mantém os fluxos de usuário gratuitos e impede débitos SwapCoin.

## Controles reforçados nesta revisão

- histórico de chat exige que a pessoa autenticada pertença à conversa;
- a consulta de histórico usa somente os parâmetros realmente presentes na JPQL;
- chat REST, voz, arquivos e WebSocket aplicam salvaguarda para contato adulto–menor sem aula registrada;
- token LiveKit exige participante da aula, aula não cancelada, horário definido e janela temporal configurada;
- chatbot/IA opcional não é liberado para menor com confirmação de responsável pendente;
- política pública informa controlador, contato, DPO e jurisdição por variáveis de ambiente;
- foram adicionados governança, matriz de bases legais, RIPD/DPIA, segurança de menores, registro de fornecedores, runbook de exclusão, plano de testes e validação de produção.

Esses controles foram preparados por inspeção do código. Ainda não constituem teste de runtime, parecer jurídico, DPA assinado, validação documental de responsável ou homologação de provedor.

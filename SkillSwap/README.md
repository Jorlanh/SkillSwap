# SkillSwap

Plataforma web de aprendizagem acessível, troca de habilidades, comprovação prática e conexão com oportunidades profissionais. O produto atende usuários comuns e mantém foco prioritário em pessoas com deficiência, sem separar a experiência principal.

Declaração de autoria: a equipe utilizou inteligência artificial somente para correção ortográfica, pontuação e revisão formal. Requisitos, decisões de produto, arquitetura, código, referências, dados e validação são responsabilidade dos cinco autores e devem ser conferidos pela equipe e pelo orientador.

## Escopo final

Esta entrega é exclusivamente de software. A alternativa de hardware foi retirada do TCC e não é requisito, dependência, protótipo, desenho ou promessa desta versão.

## Produto e arquitetura

- frontend React, TypeScript e Vite;
- backend Spring Boot em monólito modular;
- PostgreSQL com JPA e migrações Flyway;
- Firebase Authentication para identidade e Firebase Cloud Messaging para notificações quando habilitado;
- Gemini configurável no backend para chatbot, tradução, simplificação e moderação (`gemini-3.8-flash` com fallback configurável);
- recomendações determinísticas com pontuação e explicação;
- módulo `/acesso` como jornada completa de oportunidades: plano, etapas, projeto, evidência e candidatura;
- segurança por JWT validado no servidor, autorização por papel, WebSockets autenticados na primeira mensagem e ausência de token em query string;
- Actuator e Prometheus para métricas técnicas; Grafana é opcional;
- Railway como alvo de hospedagem do MVP, usando serviços gratuitos ou créditos institucionais quando disponíveis.

## Módulo SkillSwap

A tela autenticada `/acesso` apresenta o dashboard da jornada e consome os contratos de `GET/POST/PATCH /api/opportunity-os/**`, além das recomendações e oportunidades existentes. O cadastro de oportunidades por `POST /api/access/opportunities` exige `ADMIN`. A migração `V5__skillswap_access.sql` cria a base de oportunidades e `V6__opportunity_os.sql` cria plano, etapas, evidências e candidaturas.

A recomendação inicial compara habilidades e interesses declarados com os requisitos da oportunidade. A resposta apresenta pontuação, fatores e apoio de acessibilidade. Isso é um mecanismo explicável para cold start, não uma decisão automatizada de contratação e não uma prova de superioridade antes da validação.

O fluxo agora fecha o ciclo: o usuário define uma meta, executa quatro etapas, registra uma entrega verificável, solicita revisão e se candidata a um desafio. A evidência continua autodeclarada até passar por revisão; a plataforma não inventa certificações nem promete emprego.

## Execução local

1. Copie `.env.example` para um arquivo local e preencha somente variáveis privadas.
2. Copie `frontend/.env.example` para `frontend/.env.local`.
3. Suba PostgreSQL e, se necessário, Redis.
4. Execute `backend/mvnw.cmd spring-boot:run`.
5. Execute `npm ci` e `npm run dev` em `frontend`.

Em um deploy real, configure os segredos no Railway. Nunca publique `.env`, `properties.txt`, `Livekit.txt`, chaves privadas, tokens FCM. As credenciais compartilhadas durante a preparação devem ser revogadas e substituídas antes de qualquer uso externo.

## Documentação

- `docs/SKILLSWAP_ACCESS_SPEC.md`: requisitos e proposta de valor;
- `docs/ARCHITECTURE.md`: arquitetura e diagrama de software;
- `docs/IEEE_DIFFERENTIAL.md`: estado da arte, referências IEEE e hipótese de diferencial;
- `docs/DEPLOYMENT_CHECKLIST.md`: checklist do Railway e opções gratuitas;
- `docs/PROMETHEUS.md`: finalidade da observabilidade;
- `docs/GEMINI_CONFIGURATION.md`: configuração segura do Gemini 2.5 Flash;
- `docs/OPPORTUNITY_OS.md`: contratos, jornada e limites do módulo de oportunidades;
- `docs/SECURITY_ROTATION_REQUIRED.md`: resposta a credenciais expostas;
- `docs/tcc/SkillSwap_Access_TCC.docx` e `.pdf`: documentação acadêmica;
- `docs/tcc/SkillSwap_TCC_Overleaf`: fonte no modelo de artigo UCSAL;
- `docs/tcc/CARTA_PROPOSTA_EQUIPE.md` e `.docx`: proposta para aprovação da equipe.

## Limites honestos da entrega

O pacote contém implementação e contratos, mas integração com Firebase, PostgreSQL, Railway, vídeo e notificações ainda precisa de credenciais de homologação e evidência reproduzível. Não foram inventados resultados de usuários, métricas de impacto ou certificações.


## Acesso gratuito e acessibilidade

A SkillSwap não cobra dos usuários. As rotas de carteira, SwapCoins, premium e Stripe estão bloqueadas na API e não aparecem na navegação do frontend. O código histórico dessas integrações foi mantido apenas para rastreabilidade da evolução, mas não integra o produto gratuito.

Após o cadastro, a pessoa pode escolher preferências de acessibilidade, como contraste intenso, tamanho do texto, canal de comunicação, legendas quando disponíveis, descrição de imagens, VLibras e operação por teclado. Essas escolhas não diagnosticam nem classificam deficiência.

A conclusão de uma aula depende da confirmação do aluno e do professor. Depois das duas confirmações, o aluno pode gerar uma página acessível de certificado e salvá-la como PDF pelo navegador.

## Fluxo do primeiro acesso e comunidades

No primeiro acesso com uma conta Firebase, a API cria ou sincroniza o usuário local e o seu perfil no PostgreSQL. Em seguida, a pessoa escolhe um marcador visual de acessibilidade por cor e forma: azul com triângulo, laranja com quadrado ou branco com círculo. A escolha é voluntária, pode ser alterada depois e não representa diagnóstico ou classificação médica. O marcador salvo aparece no perfil.

Qualquer pessoa autenticada pode criar uma comunidade informando nome e descrição. A comunidade é gravada no banco e quem a criou entra como primeiro membro. A tela de comunidades e a barra lateral consultam a API; quando não houver registros, a interface informa isso em vez de preencher exemplos.

As permissões de câmera, microfone e localização são solicitadas pelo navegador somente quando a pessoa ativa cada opção. O navegador é quem controla a concessão e a revogação. Contatos não são solicitados porque não existe uma permissão geral segura para isso em navegadores.

O arquivo `frontend/public/firebase-messaging-sw.js` é publicado na raiz do build. As notificações não são solicitadas automaticamente durante o login; quando forem habilitadas por uma ação explícita, o frontend verifica se o arquivo foi entregue como JavaScript antes de registrá-lo.

A migração `V8__accessibility_profile_marker.sql` cria o campo do marcador visual. O Flyway executa essa migration quando o backend inicia com um PostgreSQL configurado e, depois, o Hibernate valida o modelo.

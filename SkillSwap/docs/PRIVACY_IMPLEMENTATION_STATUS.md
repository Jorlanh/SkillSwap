# Implementação de privacidade e direitos do titular

Este documento relaciona os pedidos de privacidade ao comportamento entregue no código.

| Item | Implementação | Limite declarado |
|---|---|---|
| Política acessível em português e inglês | `/privacidade` possui seletor de idioma e conteúdo nos dois idiomas | Identidade do controlador e contato precisam ser preenchidos |
| Consentimento versionado | `ss_privacy_consents`, finalidade, versão, idioma, origem, data e revogação | A equipe deve revisar as bases legais |
| Exportação | `GET /api/privacy/me/export` gera os dados disponíveis ao titular | Arquivos binários são indicados por metadados; cada fornecedor externo precisa ser conferido |
| Correção | `PATCH /api/privacy/me` e Centro de Privacidade | Campos permitidos são intencionalmente limitados |
| Exclusão | `DELETE /api/privacy/me` anonimiza a conta local e o frontend solicita a remoção no Firebase | Retenção legal, backups e fornecedores externos exigem procedimento próprio |
| Revogação | `POST /api/privacy/me/consents/{purpose}/revoke` para integrações opcionais | Revogar não apaga registros necessários à segurança |
| Retenção | `docs/RETENTION_POLICY.md` e anonimização local | Prazos finais dependem da decisão do controlador |
| Inventário | `docs/DATA_INVENTORY_AND_TRANSFERS.md` | Atualizar quando um fornecedor for ativado |
| Incidentes | `docs/INCIDENT_RESPONSE.md` | Responsáveis e canais ainda precisam ser preenchidos |
| Menores | cadastro de menores permitido; `ss_guardian_consents`, status `PENDING_GUARDIAN`/`APPROVED`, link de uso único e confirmação pública | E-mail valida controle do endereço, não prova sozinho identidade ou autoridade legal; requisitos locais precisam de revisão |
| Informações de acessibilidade | marcador privado por padrão; exposição pública somente por escolha explícita | Não inferir diagnóstico a partir do marcador |
| Exposição da entidade `User` | perfil público usa `UserPublicProfileDTO`; segredos têm proteção adicional; perfil privado não expõe bio, contatos ou habilidades | Outras respostas devem continuar usando DTOs |
| `PUT /api/users/{id}` | somente a própria conta ou `ADMIN`; contas comuns não alteram campos administrativos e a rota recebe `UserUpdateRequest`, não a entidade JPA | Manter testes de autorização |
| Transferências internacionais | inventário e política registram fornecedor, país, contrato e mecanismo | A equipe deve preencher dados reais de cada provedor |
| Revisão profissional | checklist e adendo documentam a necessidade | Não é certificação jurídica automática |
| Salvaguarda em chat e vídeo | chat REST, voz, arquivos e WebSocket restringem contato adulto–menor sem aula; LiveKit exige participante, aula não cancelada e janela de horário | Requer testes reais e revisão das regras por país |
| Histórico de conversa | a pessoa autenticada precisa ser uma das partes; a consulta JPQL foi alinhada aos parâmetros reais | Manter teste de autorização no pipeline |
| Governança e impacto | `GOVERNANCE_CONTACTS.md`, `LEGAL_BASES_MATRIX.md` e `DPIA_RIPD.md` | nomes, aprovação e parecer ainda dependem da equipe/controlador |

## Plataforma gratuita

`APP_COMMERCE_ENABLED=false` é o padrão. Nesse modo, recursos anteriormente chamados de premium não debitam SwapCoin nem pedem checkout ao usuário. O núcleo de troca, aulas, chat, certificados e acessibilidade permanece gratuito. O custo operacional de provedores externos não muda essa decisão de produto.

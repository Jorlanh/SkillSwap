# RIPD/DPIA do SkillSwap

Este registro é uma análise de impacto inicial para revisão do controlador, do encarregado/DPO e de profissional jurídico. Ele não é uma certificação de conformidade.

## 1. Descrição

O SkillSwap é uma plataforma de troca de habilidades e aprendizagem entre pares. O foco é reduzir barreiras de acesso para pessoas com deficiência e para pessoas com baixa ou nenhuma renda. O produto oferece cadastro, perfil, habilidades, propostas, chat, aulas, vídeo opcional, calendário opcional, acessibilidade de interface, certificados de aula e assistentes de IA opcionais.

## 2. Dados e tratamentos de maior risco

- dados de conta e autenticação;
- mensagens, arquivos e metadados de comunicação;
- dados de menores e informações de responsável;
- preferências de acessibilidade que podem revelar condição por contexto;
- uso de fornecedores fora do país;
- processamento de texto por Gemini/Groq, quando habilitado;
- sala de vídeo e permissões de câmera/microfone;
- logs técnicos, incidentes e pedidos de titulares.

## 3. Riscos e controles

| Risco | Impacto | Controles atuais | Risco residual | Ação antes da produção |
|---|---|---|---|---|
| Adulto contatar menor sem relação | assédio ou dano | chat/voz/arquivo exigem aula registrada; vídeo exige aula e janela | médio | teste com contas controladas, denúncia e revisão jurídica |
| E-mail de responsável não representar autoridade | consentimento inválido | transparência sobre limite; token único e expirável | alto | definir verificação proporcional por país |
| Vazamento de dados por perfil | exposição de dados | DTO público e campos sensíveis fora do JSON | médio | teste de contrato e revisão de logs |
| IA receber informação desnecessária | perda de controle ou transferência | consentimento opcional e minimização | médio | DPA, filtro de entrada, retenção e opt-out |
| Exclusão não alcançar backup | retenção indevida | anonimização local e runbook de fornecedor | médio | executar restauração e política de expiração |
| Sala de vídeo acessada fora da aula | exposição de voz/imagem | participante, status e janela temporal | baixo/médio | teste entre dois navegadores e auditoria |
| Falha de provedor | indisponibilidade | integrações opcionais e fallback textual quando existente | médio | plano de degradação e monitoramento |

## 4. Necessidade e proporcionalidade

O núcleo de troca e aula não precisa de diagnóstico médico, biometria ou renda para funcionar. Esses dados não devem ser coletados para personalizar a interface. Recursos de alto risco, como vídeo e IA, são opcionais, precisam de consentimento e devem permanecer desativados quando não houver fornecedor, contrato ou teste.

## 5. Salvaguardas

1. minimização e finalidade explícita;
2. separação entre perfil público e dados privados;
3. autenticação Firebase validada no backend;
4. autorização por participante, titular e papel;
5. consentimento versionado e revogável;
6. anonimização da conta e procedimento para fornecedores;
7. proteção específica de menores;
8. canal de incidente, denúncia e bloqueio;
9. logs sem tokens, senhas, conteúdo de mensagens ou diagnósticos;
10. testes de teclado, leitores de tela, segurança e recuperação.

## 6. Aprovação

- Controlador: PREENCHER
- Encarregado/DPO: PREENCHER
- Data da análise: PREENCHER
- Parecer jurídico: PENDENTE
- Decisão: PENDENTE / APROVADO COM RESSALVAS / REVISAR
- Data de revisão: PREENCHER

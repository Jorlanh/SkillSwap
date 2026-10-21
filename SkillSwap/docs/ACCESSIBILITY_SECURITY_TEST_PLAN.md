# Plano de testes de acessibilidade e segurança

Este arquivo é um roteiro de execução. A presença do roteiro não significa que os testes já foram concluídos.

## Acessibilidade

| ID | Tarefa | Tecnologia | Resultado esperado | Evidência | Estado |
|---|---|---|---|---|---|
| A01 | cadastrar, entrar e abrir o feed somente com teclado | teclado físico | foco visível e ordem lógica | gravação/checklist | pendente |
| A02 | completar aula e acessar certificado | teclado físico | todos os controles alcançáveis | checklist | pendente |
| A03 | usar perfil e chat | NVDA + Firefox/Chrome | nome, estado e erro anunciados | gravação/anotações | pendente |
| A04 | usar login, aula e consentimento | TalkBack + Android | leitura, foco e toque acessíveis | aparelho/data | pendente |
| A05 | usar login, aula e vídeo | VoiceOver + iOS/macOS | rótulos e mudanças de estado anunciados | aparelho/data | pendente |
| A06 | alto contraste, zoom 200% e 400% | navegador | sem perda de conteúdo essencial | capturas | pendente |
| A07 | câmera/microfone bloqueados | dois navegadores | mensagem textual e recuperação | log | pendente |
| A08 | legenda/texto alternativo em vídeo | LiveKit | participação sem áudio quando possível | roteiro | pendente |
| A09 | baixa largura de banda | throttling | fallback claro e sem travar o núcleo | vídeo/log | pendente |
| A10 | usuário real com deficiência | sessão consentida | barreiras registradas e corrigidas | termo/anotações | pendente |

## Segurança

- pedir token LiveKit para aula de outra pessoa;
- pedir histórico de conversa de terceiros;
- enviar `senderId` diferente no REST e no WebSocket;
- iniciar contato adulto–menor sem aula registrada;
- tentar vídeo fora da janela, com aula cancelada ou com `roomName` inventado;
- revogar consentimento de vídeo, IA e calendário;
- solicitar exclusão e confirmar anonimização;
- testar rate limit em uma instância e em múltiplas instâncias com Redis;
- testar CORS, logs, Actuator, migrações e ausência de segredos no ZIP;
- verificar denúncia, bloqueio e resposta de incidente.

Cada caso deve conter data, ambiente, contas sintéticas, entrada, resultado esperado, resultado observado e responsável.

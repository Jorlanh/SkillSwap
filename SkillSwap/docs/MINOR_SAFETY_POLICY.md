# Política de segurança para menores

Pessoas menores de 18 anos podem usar o SkillSwap. A proteção não funciona pela simples exclusão do menor: ela combina acesso educacional, transparência, consentimento quando exigido e limites para interações de maior risco.

## Regras implementadas

- o cadastro solicita data de nascimento;
- abaixo de 18 anos, o cadastro solicita nome, e-mail e vínculo do responsável;
- o responsável recebe um link individual, de uso único e com validade de 72 horas;
- somente o hash do token é armazenado no PostgreSQL;
- a conta continua acessível enquanto o status estiver pendente;
- a API não expõe a data de nascimento ou os dados do responsável no perfil público;
- adulto e menor não iniciam chat, voz ou envio de arquivo por contato arbitrário; a interação precisa estar vinculada a uma aula registrada;
- a videochamada só recebe token quando a pessoa participa da aula, a aula não está cancelada e o horário está dentro da janela configurada;
- o chatbot/IA opcional não é liberado para menor pendente de confirmação;
- o WebSocket deriva o remetente do JWT e rejeita mensagens que tentem usar outro identificador;
- denúncias e bloqueios existentes continuam sendo canais de proteção.

## Faixas etárias e países

O MVP usa a faixa operacional “menor de 18” para não permitir que a interface trate crianças como adultos. Isso não significa que 18 anos seja a idade jurídica universal para consentimento digital. Antes de abrir o serviço em outro país, o controlador deve definir, com apoio jurídico, pelo menos:

1. idade local para consentimento digital;
2. quando é obrigatório consentimento parental verificável;
3. quais recursos ficam disponíveis para crianças mais novas;
4. método proporcional para verificar autoridade do responsável;
5. regras de publicidade, perfilização, gravação e uso de IA;
6. idioma, canal de suporte e autoridade de proteção competente.

Enquanto essa decisão não existir, a configuração `PRIVACY_JURISDICTION` deve refletir somente o país aprovado para o piloto. Não usar o fluxo brasileiro como se fosse universal.

## Condutas proibidas

São proibidos assédio, sexualização, pedido de imagens íntimas, tentativa de retirar a conversa da plataforma para burlar salvaguardas, exposição de dados do menor, ameaça, discriminação e coleta de dados desnecessários. A plataforma deve disponibilizar denúncia, bloqueio e preservação mínima de evidências.

## Limites conhecidos

O link por e-mail comprova controle do endereço informado, mas não prova sozinho identidade ou autoridade legal. O sistema não faz verificação documental nem avaliação humana automática. Essa limitação deve ser comunicada e resolvida pelo controlador antes da publicação comercial ou em países que exijam garantia adicional.

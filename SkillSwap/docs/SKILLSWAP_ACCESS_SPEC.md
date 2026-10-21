# SkillSwap Access — especificação consolidada

## Declaração de autoria e uso de IA

Nós, integrantes da equipe, utilizamos IA somente para correção ortográfica, pontuação e revisão formal da redação. As decisões de produto, requisitos, arquitetura, código, seleção bibliográfica, interpretação de dados e validação são responsabilidade da equipe.

## Visão do produto

O SkillSwap Access organiza uma jornada que normalmente aparece fragmentada: aprender, praticar, comprovar uma habilidade, encontrar uma pessoa ou instituição e alcançar uma oportunidade. Usuários comuns podem ensinar e aprender; pessoas com deficiência são prioridade de acessibilidade; universidades, empresas e instituições podem contratar funções institucionais.

## Problema

Conteúdo, comunidades, cursos, portfólios, vagas e recursos de acessibilidade frequentemente vivem em sistemas separados. A pessoa repete informações e precisa explicar suas necessidades em cada etapa. Instituições também têm dificuldade para relacionar capacitação, evidência prática, acessibilidade e oportunidade sem coletar dados em excesso.

Pergunta de pesquisa: como uma plataforma acessível pode conectar aprendizagem, evidência prática, recomendação explicável e oportunidades profissionais preservando autonomia, privacidade e participação de pessoas com e sem deficiência?

## Funcionalidades

- formação em texto, áudio, legendas e linguagem simples;
- atividades, desafios e projetos que produzem evidências práticas;
- perfil de habilidades oferecidas e desejadas;
- recomendações explicáveis de cursos, mentores, projetos e oportunidades;
- comunidades, publicações, propostas, chat, arquivos e vídeo opcional;
- painel institucional com indicadores agregados de capacitação e inclusão;
- preferências de acessibilidade, leitura em voz alta, teclado, contraste e VLibras opcional;
- FCM opcional para notificações;
- economia de créditos e pagamentos com webhook assinado quando habilitados;
- plano persistido de evolução com objetivo, ritmo e limitações;
- etapas práticas com estados de execução;
- portfólio de evidências com link opcional e pedido de revisão;
- candidatura rastreável a projetos e oportunidades;
- auditoria, observabilidade e controles administrativos.

## Arquitetura escolhida

O TCC e o MVP usam monólito modular Spring Boot. Os módulos têm limites explícitos: identidade, perfil, aprendizagem, recomendações, oportunidades, comunidades, chat, notificações, carteira, pagamentos, administração e observabilidade. React/Vite é o cliente; PostgreSQL/Flyway é a persistência; Firebase é a identidade. Microsserviços podem ser extraídos depois de medir carga e fronteiras, mas não são necessários para demonstrar valor.

## Recomendação explicável

A primeira regra normaliza habilidades e interesses, separa requisitos por vírgula, identifica correspondências exatas ou parciais e adiciona pesos documentados para apoio de acessibilidade e modalidade remota. O resultado é limitado e retorna motivos. Não decide contratação, não usa deficiência para excluir e deve permitir revisão humana.

## Modelo de negócio

O caminho B2B2C prevê assinatura institucional, implantação, integração com RH e educação, relatórios e suporte. O usuário comum deve ter uma jornada inicial útil; recursos pagos não podem bloquear acessibilidade básica. A hipótese comercial depende de entrevistas e pilotos, não de uma promessa de faturamento.

## Jornada Opportunity OS

O módulo `/acesso` transforma a proposta em uma experiência operacional: o usuário cria um plano, executa etapas, publica evidências e se candidata a oportunidades. A geração inicial é determinística para funcionar sem dependência de chave de IA; o ELOS/Gemini pode orientar o usuário, mas não substitui revisão humana nem certifica competência.

## Validação

Antes de alegar impacto, a equipe deve medir tempo, erros, abandono, compreensão da recomendação, satisfação, acessibilidade percebida, latência, confiabilidade e segurança. O protocolo, a amostra e os resultados devem ser aprovados pelo orientador e registrados sem inventar dados.

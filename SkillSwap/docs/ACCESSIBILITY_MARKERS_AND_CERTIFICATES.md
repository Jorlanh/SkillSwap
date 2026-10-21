# Alto contraste, marcador visual e certificados

## Alto contraste

A SkillSwap disponibiliza um modo de alto contraste que pode ser ativado na barra de acessibilidade ou em Preferências. O modo escurece as áreas principais, aumenta a visibilidade do texto, usa vermelho claro como cor de destaque e acrescenta bordas e foco visível aos elementos interativos.

O modo é uma preferência da interface. Ele não substitui a avaliação de contraste em cada tela e não deve ser apresentado como prova isolada de conformidade WCAG. A navegação continua disponível por teclado e os controles possuem rótulos textuais.

## Marcador visual escolhido pelo usuário

O usuário pode escolher uma das combinações abaixo:

- azul com triângulo;
- laranja com quadrado;
- branco com círculo.

O marcador é uma preferência de apresentação escolhida pela própria pessoa. A aplicação não usa essas opções para diagnosticar, inferir ou expor deficiência invisível, deficiência visível, autismo, daltonismo ou qualquer outra condição sensível. A mesma opção pode ser usada por qualquer pessoa, e cada marcador possui texto alternativo acessível. A forma não é usada sozinha para comunicar uma decisão importante.

O valor é persistido em `tb_users.accessibility_visual_marker` pela migration `V8__accessibility_markers_and_lesson_certificates.sql` e atualizado pelo endpoint autenticado `PATCH /api/settings/accessibility/update`.

## Certificado de conclusão

Ao concluir uma aula, a rota autenticada `POST /api/lesson/{lessonId}/complete` registra o estado `COMPLETED`, a data de conclusão e um código único. O certificado pode ser consultado por `GET /api/lesson/certificate/{lessonId}` somente por um dos participantes da aula.

O certificado apresenta:

- nome do aluno;
- nome de usuário do aluno;
- nome do professor;
- título e introdução da aula;
- carga horária calculada a partir de `duration_minutes`;
- data de conclusão;
- código de verificação.

O frontend oferece a ação “Gerar certificado” na página de aulas. O navegador abre uma versão pronta para impressão e permite salvar como PDF. Isso evita colocar uma chave de serviço de geração de PDF no frontend e mantém os dados do certificado sob autorização do backend.

O certificado comprova a conclusão daquela aula na plataforma. Ele não equivale a uma certificação profissional, diploma ou reconhecimento oficial externo.

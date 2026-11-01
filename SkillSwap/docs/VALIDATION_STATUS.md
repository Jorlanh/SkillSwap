# Status de validação da entrega Opportunity OS

## Verificações concluídas

- Backend: `mvnw.cmd -q -DskipTests compile` passou.
- Backend empacotado: `mvnw.cmd -q -Dmaven.test.skip=true -Powasp-skip package` passou e gerou `skill-swap-0.0.1-SNAPSHOT.jar`.
- Frontend: `tsc -p tsconfig.app.json --noEmit --pretty false` passou.
- Frontend: `npm run lint` passou sem erros; os avisos existentes são de tipagem gradual, Fast Refresh e hooks do código legado.
- JAR: conferido com as classes do `OpportunityOsController`, `OpportunityOsService`, entidades novas e `V6__opportunity_os.sql`.

## Limitações do ambiente desta sessão

O `vite build` não pôde ser concluído porque o processo nativo do esbuild foi bloqueado pela sandbox ao resolver diretórios ascendentes, antes de ler a configuração do projeto. A verificação TypeScript e o lint passaram. Em uma máquina de desenvolvimento normal, execute `npm ci` e `npm run build` dentro de `frontend`.

Os testes Maven legados também não chegaram à execução: o `testCompile` do checkout original não resolveu os pacotes do próprio `target/classes`, embora a compilação principal tenha passado. Isso foi mantido explícito para não apresentar validação de testes como se tivesse ocorrido.

## Homologação necessária

Para validar runtime, configure PostgreSQL, Firebase Authentication e as variáveis privadas de `backend/src/main/resources/application.properties` via ambiente. Flyway deve aplicar V1 a V6; não use `ddl-auto=update`.

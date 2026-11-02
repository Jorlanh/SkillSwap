# Estágio 1: build da aplicação
FROM eclipse-temurin:21-jdk AS build

RUN apt-get update \
    && apt-get install -y --no-install-recommends maven \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src

RUN mvn -B clean package -DskipTests -Powasp-skip


# Estágio 2: execução da aplicação
FROM eclipse-temurin:21-jre

COPY --from=build /app/target/skill-swap-*.jar /app.jar

RUN groupadd --system app \
    && useradd --system --gid app app \
    && chown app:app /app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app.jar"]
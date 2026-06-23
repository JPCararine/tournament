# ---------------------------------------------------------------------------
# Estágio 1 — build (Maven + JDK 21)
# ---------------------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Baixa as dependências primeiro (camada cacheável enquanto o pom não muda).
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Compila e empacota (sem testes — eles dependem de infra externa).
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------------------------------------------------------------------------
# Estágio 2 — runtime (apenas JRE 21, imagem enxuta)
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Usuário sem privilégios.
RUN useradd -r -u 1001 appuser

COPY --from=build /app/target/tournament-backend-*.jar app.jar

EXPOSE 8080
USER appuser

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

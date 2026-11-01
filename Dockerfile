# --- Build ---------------------------------------------------------------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Copia só o que define as dependências primeiro, para o Docker reaproveitar essa camada.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -q dependency:go-offline

COPY src src
RUN ./mvnw -q package -DskipTests

# --- Execução ------------------------------------------------------------
FROM eclipse-temurin:25-jre
WORKDIR /app

# Não roda como root dentro do contêiner.
RUN useradd --system --uid 10001 mototrack
USER mototrack

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080

# A plataforma define PORT; o app já lê essa variável (server.port=${PORT:8080}).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]

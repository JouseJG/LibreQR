# ---- Etapa de compilación ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Se copian primero el pom y los fuentes para aprovechar la caché de dependencias.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- Etapa de ejecución ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# curl para el healthcheck del docker-compose.yml
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

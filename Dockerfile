# ── Build ─────────────────────────────────────────────────────────────────────
# Imagen con Maven + JDK 21 para compilar el proyecto
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Descargamos dependencias antes de copiar el fuente
# (aprovecha la caché de Docker si pom.xml no cambia)
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Runtime ───────────────────────────────────────────────────────────────────
# Solo JRE alpine: imagen final liviana, sin Maven ni código fuente
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Usuario sin privilegios para no correr como root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /app/target/*.jar auth-service.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "auth-service.jar"]
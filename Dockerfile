# ================================================================
# Dockerfile — Microservicio Auth
# ================================================================


# ── Stage 1: Build ───────────────────────────────────────────────
# Maven + JDK 21 Alpine: solo para compilar, no va a producción
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# Copiamos pom.xml primero para aprovechar caché de capas de Docker
# Si el código fuente cambia pero pom.xml no, las dependencias no se re-descargan
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copiamos el fuente y empaquetamos el JAR (sin ejecutar tests)
COPY src ./src
RUN mvn clean package -DskipTests -q


# ── Stage 2: Runtime ─────────────────────────────────────────────
# Solo JRE Alpine: imagen final liviana sin Maven, JDK ni código fuente expuesto
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Creamos usuario sin privilegios para no ejecutar el proceso como root
# Buena práctica de seguridad en contenedores de producción
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copiamos únicamente el JAR generado en el stage anterior
COPY --from=build /app/target/*.jar auth-service.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "auth-service.jar"]

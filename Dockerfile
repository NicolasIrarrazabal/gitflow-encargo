# ── Etapa 1: Compilación ──────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Descargamos las dependencias antes de copiar el código fuente.
# Así Docker reutiliza esta capa en builds posteriores si el pom.xml no cambia.
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Ahora sí copiamos el fuente y compilamos
COPY src ./src
RUN mvn clean package -DskipTests -q

# ── Etapa 2: Imagen final liviana ─────────────────────────────────────────────
# Solo el JRE, nada de Maven ni fuentes (~200 MB vs ~700 MB de la imagen Maven)
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Solo copiamos el JAR; todo lo demás se queda en la etapa de build
COPY --from=build /app/target/auth-service.jar app.jar

# Creamos un usuario sin privilegios para no correr como root
RUN useradd -r -u 1001 appuser && chown appuser /app
USER appuser

EXPOSE 8080

# El healthcheck le da tiempo al Spring Boot de arrancar antes de marcar la imagen como unhealthy
HEALTHCHECK --interval=10s --timeout=5s --retries=5 --start-period=30s \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]

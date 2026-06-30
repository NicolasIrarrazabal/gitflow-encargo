# Build multi-stage: stage 1 compila con Maven, stage 2 corre solo con JRE.

FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

# pom.xml primero para aprovechar cache de capas si el código cambia pero
# las dependencias no
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn clean package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# wget para los health checks de Docker
RUN apk add --no-cache wget curl

# usuario sin privilegios, no corremos como root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

RUN mkdir -p /app/logs && chown -R appuser:appgroup /app/logs

USER appuser

COPY --from=build /app/target/*.jar auth-service.jar

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --retries=5 --start-period=30s \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "auth-service.jar"]
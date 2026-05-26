# ── Imagen base ───────────────────────────────────────────────────────────────
# JDK 21 de Eclipse Temurin para compilar y ejecutar el proyecto
FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copiamos el JAR compilado por Maven al contenedor
COPY target/*.jar auth-service.jar

# Puerto en el que escucha el microservicio
EXPOSE 8080

# Comando de inicio del microservicio
ENTRYPOINT ["java", "-jar", "auth-service.jar"]

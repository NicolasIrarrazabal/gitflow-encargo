# ── Imagen base ───────────────────────────────────────────────────────────────
# Maven con JDK 21 para compilar y ejecutar el proyecto
FROM maven:3.9-eclipse-temurin-21

WORKDIR /app

# Descargamos dependencias antes de copiar el fuente
# (aprovecha la caché de Docker si pom.xml no cambia)
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copiamos el código fuente y compilamos el proyecto
COPY src ./src
RUN mvn clean package -DskipTests -q

# Puerto en el que escucha el microservicio
EXPOSE 8080

# Comando de inicio del microservicio
ENTRYPOINT ["java", "-jar", "target/auth-service.jar"]

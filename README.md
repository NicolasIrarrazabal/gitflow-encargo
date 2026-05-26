# 🔐 Auth Microservice - Arquitectura DevOps y CI/CD

Este repositorio contiene el microservicio de autenticación de usuarios. En esta fase del proyecto, la aplicación ha sido contenerizada, orquestada y automatizada mediante un pipeline robusto de Integración y Entrega Continua (CI/CD).

---

## 🏗️ 1. Arquitectura de Contenedores (Docker)

El proyecto utiliza Docker y Docker Compose para garantizar la portabilidad, el aislamiento y la fácil ejecución en cualquier entorno.

**API (Spring Boot):** Se utiliza un Dockerfile optimizado con la técnica de Multi-stage build. La primera etapa utiliza Maven para compilar el código fuente, y la segunda etapa empaqueta únicamente el archivo `.jar` resultante en una imagen ligera de Java (JRE), reduciendo drásticamente el peso final y la superficie de ataque.

**Ejecución Local**

```bash
docker compose up -d --build
```

El servicio queda disponible en `http://localhost:8080`.

---

## ⚙️ 2. Pipeline CI/CD (GitHub Actions)

Se implementó un pipeline automatizado (`ci-cd.yml`) que se activa con eventos `push` hacia la rama `main`. El pipeline consta de 4 etapas críticas:

**Build & Test:** Ejecuta las pruebas unitarias y de integración de Maven (`mvn clean verify`). JaCoCo bloquea el pipeline si la cobertura cae por debajo del **70% de líneas** o **60% de ramas**.

**Análisis de Código Estático (SonarCloud):** Inspecciona el código en busca de vulnerabilidades, code smells y bugs. Existe un Quality Gate estricto que bloquea el pipeline si el código no cumple con los estándares corporativos.

**Escaneo de Dependencias (Snyk):** Analiza el archivo `pom.xml` en busca de vulnerabilidades conocidas (CVEs) en librerías de terceros. Está configurado para fallar el pipeline si detecta vulnerabilidades de nivel **ALTO o CRÍTICO** (`--severity-threshold=high`). Genera un `snyk-report.json` descargable como artefacto.

**Entrega Continua (Docker Hub):** Si (y solo si) el código pasa todas las pruebas anteriores y es fusionado a la rama `main`, el pipeline construye la imagen Docker final y la publica automáticamente en el registro público.

---

## 🔍 3. Garantía de Calidad y Trazabilidad

**Trazabilidad del Código a la Producción:** Cada imagen subida a Docker Hub es el resultado directo de un commit específico en la rama `main`. No hay despliegues manuales; todo pasa por el historial auditable de GitHub Actions.

**Calidad de Código Inquebrantable:** La integración de SonarCloud actúa como un juez imparcial. Si un desarrollador introduce código duplicado o inseguro, el Quality Gate falla automáticamente, impidiendo que ese código llegue a producción.

**Seguridad Proactiva:** Snyk analiza cada PR. Además, **Dependabot** genera pull requests automáticos semanales cuando hay actualizaciones pendientes en dependencias Maven, imágenes Docker y GitHub Actions.

---

## 🔑 4. Secrets requeridos en GitHub

| Secret | Descripción |
|---|---|
| `DOCKER_USERNAME` | Usuario de Docker Hub |
| `DOCKER_PASSWORD` | Contraseña de Docker Hub |
| `SONAR_TOKEN` | Token de autenticación de SonarCloud |
| `PROJECT_KEY_SONAR` | Project Key del proyecto en SonarCloud |
| `ORGANIZATION_SONAR` | Organización en SonarCloud |
| `SNYK_TOKEN` | Token de autenticación de Snyk |

---

## 📡 Endpoints disponibles

- `POST /auth/login` — Recibe usuario y contraseña. Si las credenciales son válidas, devuelve un token.
- `GET /auth/health` — Verifica que el servicio esté funcionando correctamente.
- `GET /actuator/health` — Health check detallado para monitoreo.

---

## 🗂️ Estructura del proyecto

```
├── src/
│   ├── main/java/com/encargo/microservicio/
│   │   ├── controller/
│   │   ├── model/
│   │   ├── service/
│   │   └── MicroservicioApplication.java
│   └── test/
├── .github/
│   ├── workflows/
│   │   └── ci-cd.yml
│   └── dependabot.yml
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

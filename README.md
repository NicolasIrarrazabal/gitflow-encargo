# 🚀 Microservicio Auth — Pipeline CI/CD

> **Evaluación Parcial N°2 · DOY0101 Ingeniería DevOps**  
> Automatización del ciclo de vida de un microservicio mediante GitHub Actions, Docker y Docker Compose.

---

## 📋 Tabla de Contenidos

- [Descripción del Proyecto](#descripción-del-proyecto)
- [Tecnologías Utilizadas](#tecnologías-utilizadas)
- [Estructura del Repositorio](#estructura-del-repositorio)
- [Arquitectura del Pipeline CI/CD](#arquitectura-del-pipeline-cicd)
- [Contenedorización (IE1)](#contenedorización-ie1)
- [Pruebas Automatizadas (IE2)](#pruebas-automatizadas-ie2)
- [Seguridad y Calidad de Código (IE3)](#seguridad-y-calidad-de-código-ie3)
- [Despliegue Automatizado (IE4)](#despliegue-automatizado-ie4)
- [Orquestación de Contenedores (IE5)](#orquestación-de-contenedores-ie5)
- [Trazabilidad y Calidad](#trazabilidad-y-calidad)
- [Secrets Requeridos](#secrets-requeridos)
- [Ejecución Local](#ejecución-local)
- [Uso de Inteligencia Artificial](#uso-de-inteligencia-artificial)

---

## Descripción del Proyecto

Este repositorio implementa un pipeline de **Integración y Entrega Continua (CI/CD)** para el microservicio de autenticación (`microservicio-auth`). El objetivo es automatizar el ciclo de vida del servicio: compilación, pruebas, análisis de seguridad, construcción de imagen Docker y despliegue en un entorno simulado.

El pipeline se ejecuta en **GitHub Actions** con las siguientes etapas:

```
Build → Pruebas Unitarias → Seguridad (SonarCloud + Snyk) → Build Docker → Despliegue Simulado
```

---

## Tecnologías Utilizadas

| Categoría | Herramienta | Versión |
|---|---|---|
| Lenguaje | Java | 21 (Temurin) |
| Build | Maven | 3.9.x |
| Framework | Spring Boot | — |
| Contenedor | Docker | JDK 21 |
| Orquestación | Docker Compose | 3.8 |
| CI/CD | GitHub Actions | — |
| Calidad | SonarCloud | — |
| Seguridad | Snyk | — |
| Dependencias | Dependabot | — |
| Cobertura | JaCoCo | — |

---

## Estructura del Repositorio

```
gitflow-encargo/
├── .github/
│   └── workflows/
│       └── ci-cd.yml          # Pipeline CI/CD principal (rama main)
├── .mvn/
│   └── wrapper/
│       └── maven-wrapper.properties
├── src/
│   ├── main/java/...          # Código fuente del microservicio
│   └── test/java/...          # Pruebas unitarias (JUnit)
├── Dockerfile                 # Imagen del microservicio
├── docker-compose.yml         # Orquestación local del servicio
├── pom.xml                    # Dependencias y configuración Maven
└── README.md
```

---

## Arquitectura del Pipeline CI/CD

El pipeline (`ci-cd.yml`) se activa con **push a la rama `main`** y ejecuta los siguientes jobs:

```
┌─────────┐     ┌──────────────────┐     ┌──────────────┬──────────────┐
│  Build  │────▶│ Pruebas Unitarias│────▶│  SonarCloud  │  Snyk Scan   │
└─────────┘     └──────────────────┘     └──────┬───────┴──────┬───────┘
                                                 │              │
                                          ┌──────▼──────────────▼──────┐
                                          │      Build Docker Image     │
                                          └─────────────┬───────────────┘
                                                        │
                                          ┌─────────────▼───────────────┐
                                          │     Despliegue Simulado      │
                                          └─────────────────────────────┘
```

Los jobs de **SonarCloud** y **Snyk** corren en **paralelo** para optimizar el tiempo del pipeline.

---

## Contenedorización (IE1)

El `Dockerfile` conteneriza el microservicio usando la imagen `eclipse-temurin:21-jdk`. Copia el JAR compilado por Maven y lo expone en el puerto 8080.

```dockerfile
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY target/*.jar auth-service.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "auth-service.jar"]
```

La imagen se construye y publica automáticamente en Docker Hub durante el pipeline, con dos tags: `latest` y el SHA del commit para identificar exactamente qué versión está desplegada.

---

## Pruebas Automatizadas (IE2)

Las pruebas unitarias se ejecutan en el job `pruebas-unitarias` usando **JUnit** a través de Maven:

```bash
mvn test
```

JaCoCo genera un reporte de cobertura de código que se sube como artefacto del pipeline, disponible en la sección **Artifacts** de cada ejecución en GitHub.

---

## Seguridad y Calidad de Código (IE3)

Dos herramientas corren en paralelo tras las pruebas unitarias:

**SonarCloud:** analiza la calidad del código detectando bugs, code smells y duplicaciones. Si el Quality Gate falla, el pipeline se bloquea y no avanza al build de Docker.

**Snyk:** escanea las dependencias del proyecto buscando vulnerabilidades conocidas con severidad alta o crítica. El reporte se guarda como artefacto del pipeline.

**Dependabot:** configurado en GitHub para proponer actualizaciones automáticas de dependencias con vulnerabilidades detectadas.

El mecanismo de bloqueo funciona mediante el campo `needs` en el job `build-docker`, que depende de que tanto `security-sonar` como `security-snyk` hayan finalizado exitosamente.

---

## Despliegue Automatizado (IE4)

El job `despliegue-simulado` levanta el microservicio usando **Docker Compose** dentro del runner de GitHub Actions:

1. Se instala Docker Compose en el runner
2. Se autentica en Docker Hub
3. Se ejecuta `docker-compose up -d` para levantar el contenedor
4. Se derriba el entorno con `docker-compose down` al finalizar

La trazabilidad se garantiza mediante el tag SHA del commit en la imagen Docker, permitiendo identificar exactamente qué versión del código está desplegada en cada ejecución.

---

## Orquestación de Contenedores (IE5)

El archivo `docker-compose.yml` orquesta el microservicio con las siguientes configuraciones:

**Política de reinicio:** `restart: always` relanza el contenedor automáticamente ante caídas.

**Healthcheck:** verifica el estado del servicio cada 10 segundos:

```yaml
healthcheck:
  test: ["CMD-SHELL", "wget --quiet --tries=1 --spider http://localhost:8080/auth/health || exit 1"]
  interval: 10s
  timeout: 5s
  retries: 5
  start_period: 30s
```

**Variables de entorno:** las configuraciones se inyectan desde el entorno del host, sin archivos `.env` versionados.

---

## Trazabilidad y Calidad

| Mecanismo | Descripción |
|---|---|
| **SHA en tag Docker** | Cada imagen publicada lleva el SHA del commit, vinculando código y despliegue |
| **Artefactos del pipeline** | Reportes de JaCoCo y Snyk quedan adjuntos a cada ejecución |
| **Dependencia entre jobs** | El campo `needs` impide saltarse etapas del pipeline |
| **Logs de GitHub Actions** | Cada paso queda registrado con timestamps y output completo |

---

## Secrets Requeridos

| Secret | Descripción |
|---|---|
| `SONAR_TOKEN` | Token de autenticación de SonarCloud |
| `PROJECT_KEY_SONAR` | Clave del proyecto en SonarCloud |
| `ORGANIZATION_SONAR` | Nombre de la organización en SonarCloud |
| `SNYK_TOKEN` | Token de autenticación de Snyk |
| `DOCKER_USERNAME` | Usuario de Docker Hub |
| `DOCKER_PASSWORD` | Contraseña o Access Token de Docker Hub |

---

## Ejecución Local

```bash
# 1. Compilar el proyecto
mvn clean package -DskipTests

# 2. Levantar con Docker Compose
docker-compose up --build

# 3. Detener
docker-compose down
```

---

## Uso de Inteligencia Artificial

| Herramienta | Uso aplicado |
|---|---|
| Claude (Anthropic) | Apoyo en la generación del README.md, estructura del pipeline y revisión técnica |

Todo el contenido generado con IA fue revisado y validado por el equipo.

> Referencia: https://bibliotecas.duoc.cl/ia

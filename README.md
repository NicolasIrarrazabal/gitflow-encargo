# Microservicio Auth — Pipeline CI/CD

> **Evaluación Parcial N°2 · DOY0101 Ingeniería DevOps**  
> Automatización del ciclo de vida de un microservicio mediante GitHub Actions, Docker y Docker Compose.

---

## Tabla de Contenidos

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
- [Conclusiones Personales](#conclusiones-personales)
- [Uso de Inteligencia Artificial](#uso-de-inteligencia-artificial)

---

## Descripción del Proyecto

Este repositorio implementa un pipeline de **Integración y Entrega Continua (CI/CD)** para el microservicio de autenticación `microservicio-auth`. El objetivo es automatizar todo el ciclo de vida del servicio: desde la compilación hasta el despliegue, pasando por pruebas con validación de cobertura, análisis de calidad y escaneo de vulnerabilidades.

El pipeline corre en **GitHub Actions** y sigue esta secuencia:

```
Build → Pruebas + Cobertura → Seguridad (SonarCloud + Snyk) → Build Docker → Despliegue Simulado
```

El microservicio expone dos endpoints: `/auth/login` para autenticación y `/auth/health` para verificaciones de disponibilidad.

---

## Tecnologías Utilizadas

| Categoría     | Herramienta      | Versión      |
|---------------|------------------|--------------|
| Lenguaje      | Java             | 21 (Temurin) |
| Build         | Maven            | 3.9.x        |
| Framework     | Spring Boot      | 3.4.3        |
| Contenedor    | Docker (JRE)     | 21-jre-jammy |
| Orquestación  | Docker Compose   | 3.8          |
| CI/CD         | GitHub Actions   | —            |
| Calidad       | SonarCloud       | —            |
| Seguridad     | Snyk             | —            |
| Dependencias  | Dependabot       | —            |
| Cobertura     | JaCoCo           | 0.8.12       |

---

## Estructura del Repositorio

```
gitflow-encargo/
├── .github/
│   ├── dependabot.yml             # PRs automáticos cada lunes con actualizaciones de dependencias
│   └── workflows/
│       └── ci-cd.yml              # Pipeline principal con los 5 jobs
├── .mvn/wrapper/
│   └── maven-wrapper.properties
├── src/
│   ├── main/java/...              # Código fuente: controller, service, model
│   └── test/java/...              # Pruebas unitarias con JUnit 5 y Mockito
├── .dockerignore                  # Evita que .git y target/ inflen el contexto de build
├── Dockerfile                     # Build multi-stage: Maven para compilar, JRE para ejecutar
├── docker-compose.yml             # Levanta el microservicio con red, healthcheck y límites de recursos
├── pom.xml                        # Dependencias y JaCoCo con umbrales 70% líneas / 60% ramas
└── README.md
```

---

## Arquitectura del Pipeline CI/CD

El pipeline se activa en cada **push a `main`** y en **Pull Requests** hacia `main`. Los jobs de seguridad corren en paralelo para no alargar el tiempo total innecesariamente.

```
┌─────────┐     ┌──────────────────────┐     ┌──────────────┬──────────────┐
│  Build  │────▶│ Pruebas + Cobertura  │────▶│  SonarCloud  │  Snyk Scan   │
└─────────┘     └──────────────────────┘     └──────┬───────┴──────┬───────┘
                                                     │              │
                                              ┌──────▼──────────────▼──────┐
                                              │    Build y Push Docker Hub  │
                                              └─────────────┬───────────────┘
                                                            │
                                              ┌─────────────▼───────────────┐
                                              │     Despliegue Simulado      │
                                              │   (microservicio en :8080)   │
                                              └─────────────────────────────┘
```

El job `build-docker` solo corre en push directo a `main`, no en PRs. Esto evita publicar imágenes de ramas en revisión.

---

## Contenedorización (IE1)

El `Dockerfile` usa **build multi-stage** para mantener la imagen final lo más liviana posible:

- **Etapa `build`**: usa `maven:3.9-eclipse-temurin-21` para compilar y empaquetar el JAR.
- **Etapa final**: usa `eclipse-temurin:21-jre-jammy` (~200 MB vs ~700 MB de la imagen Maven completa) y copia únicamente el JAR resultante.

Decisiones de seguridad y eficiencia incluidas:
- **Usuario no-root** (`appuser`, UID 1001) para no exponer el proceso como root dentro del contenedor.
- **HEALTHCHECK** integrado en la imagen para que Docker y el pipeline puedan saber cuándo el servicio está listo.
- **`.dockerignore`** configurado para excluir `.git/`, `target/` y otros archivos que no aportan nada al build.

La imagen se publica en Docker Hub con dos tags: `latest` y el **SHA del commit**, lo que permite rastrear exactamente qué código está corriendo en cada despliegue.

---

## Pruebas Automatizadas (IE2)

Las pruebas corren en el job `pruebas-unitarias` con `mvn verify` (no `mvn test`):

```bash
mvn verify
```

La razón de usar `verify` es que activa la fase de validación de **JaCoCo**, que corta el build si la cobertura baja de:
- **70% de líneas** cubiertas.
- **60% de ramas** cubiertas.

El reporte HTML de JaCoCo se sube como artefacto del pipeline en cada ejecución y queda disponible en la pestaña *Actions* de GitHub.

El pipeline también valida los PRs antes de permitir el merge, así no entra código sin tests al branch principal.

---

## Seguridad y Calidad de Código (IE3)

Tras las pruebas, corren tres mecanismos en paralelo:

**SonarCloud** analiza bugs, code smells y duplicaciones. El parámetro `sonar.qualitygate.wait=true` hace que el job espere el resultado del Quality Gate antes de terminar — si no pasa, el pipeline se detiene y `build-docker` nunca se ejecuta.

**Snyk** escanea las dependencias Maven en busca de vulnerabilidades de severidad `high` o `critical`. El reporte JSON queda como artefacto. Si Snyk encuentra algo grave, el pipeline para ahí.

**Dependabot** está configurado en `.github/dependabot.yml` para abrir PRs automáticos cada lunes con actualizaciones de dependencias Maven, Docker y GitHub Actions.

El bloqueo entre etapas funciona por el campo `needs` en `build-docker`, que requiere que ambos jobs de seguridad terminen con éxito.

---

## Despliegue Automatizado (IE4)

El job `despliegue-simulado` levanta el stack dentro del runner de GitHub Actions:

1. Se autentica en Docker Hub.
2. Ejecuta `docker compose up -d` inyectando `IMAGE_TAG=${{ github.sha }}` — usa la imagen ya publicada, no reconstruye desde cero.
3. Espera hasta 60 segundos (12 reintentos de 5s) hasta que `/auth/health` responda.
4. Verifica que `/auth/login` devuelva HTTP 200 con credenciales válidas.
5. Derriba el entorno con `docker compose down`.

El tag SHA garantiza que la imagen desplegada es exactamente la misma que pasó los análisis de seguridad.

---

## Orquestación de Contenedores (IE5)

El `docker-compose.yml` configura el microservicio con aislamiento de red y límites de recursos explícitos:

| Servicio             | Puerto expuesto | Red       |
|----------------------|-----------------|-----------|
| `microservicio-auth` | 8080            | `backend` |

**Red:** la red `backend` con driver bridge aísla el servicio del exterior, salvo por el puerto 8080.

**Recursos:**
- Límite: 0.5 CPU / 512 MB RAM
- Reserva: 0.25 CPU / 256 MB RAM

**Healthcheck:** el contenedor se verifica cada 10s contra `/actuator/health`. Spring Boot tiene 30s de gracia para arrancar antes de que empiece a contar.

**Reinicio automático:** `restart: always` recupera el servicio si el proceso muere inesperadamente.

---

## Trazabilidad y Calidad

| Mecanismo                  | Qué garantiza                                                              |
|----------------------------|----------------------------------------------------------------------------|
| **SHA en tag Docker**      | Vincula exactamente qué commit está corriendo en cada despliegue           |
| **Imagen reutilizada**     | El job de despliegue usa la imagen publicada, no reconstruye               |
| **Artefactos del pipeline**| Reportes de JaCoCo y Snyk adjuntos a cada ejecución en GitHub             |
| **Dependencia entre jobs** | `needs` impide avanzar si alguna etapa falla                               |
| **Quality Gate activo**    | `sonar.qualitygate.wait=true` bloquea si SonarCloud no aprueba            |
| **Umbral JaCoCo activo**   | `mvn verify` falla automáticamente si la cobertura queda bajo 70%/60%     |
| **Validación en PRs**      | Build y tests corren antes de cualquier merge a main                       |

---

## Secrets Requeridos

Estos secrets deben estar configurados en *Settings → Secrets and variables → Actions* del repositorio:

| Secret                | Para qué se usa                                    |
|-----------------------|----------------------------------------------------|
| `SONAR_TOKEN`         | Autenticación con SonarCloud                       |
| `PROJECT_KEY_SONAR`   | Identificador del proyecto en SonarCloud           |
| `ORGANIZATION_SONAR`  | Nombre de la organización en SonarCloud            |
| `SNYK_TOKEN`          | Autenticación con Snyk                             |
| `DOCKER_USERNAME`     | Usuario de Docker Hub                              |
| `DOCKER_PASSWORD`     | Contraseña o Access Token de Docker Hub            |

---

## Ejecución Local

```bash
# 1. Compilar y empaquetar
mvn clean package -DskipTests

# 2. Levantar con Docker Compose (requiere imagen en Docker Hub o build local)
DOCKER_USERNAME=tu_usuario IMAGE_TAG=local docker compose up --build

# 3. Probar el login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"1234"}'

# 4. Verificar que el servicio está vivo
curl http://localhost:8080/auth/health

# 5. Detener todo
docker compose down
```

Para correr solo las pruebas con reporte de cobertura:

```bash
mvn verify
# El reporte queda en target/site/jacoco/index.html
```

---

## Conclusiones Personales

Durante el desarrollo de esta evaluación trabajé de forma individual, así que tuve que hacerme cargo de todo el proceso, desde armar el pipeline hasta configurar cada herramienta que se integraba. Fue complicado en varios momentos, pero también me sirvió para entender mejor cómo funciona realmente un ciclo CI/CD completo.

Lo que más me costó fue configurar los secrets y variables de entorno en GitHub Actions, además del despliegue con Docker Compose. Con los secrets me tomó tiempo entender cómo manejar credenciales sin dejarlas expuestas en el código, y ahí me di cuenta de lo importante que es la seguridad incluso desde etapas tempranas del desarrollo. Con Docker Compose hubo bastante prueba y error hasta lograr que el pipeline levantara el contenedor correctamente, esperara a que estuviera disponible y comprobara el endpoint de salud. Cuando finalmente funcionó, sentí que había entendido mucho mejor cómo sería un despliegue en un entorno real.

Este proyecto me hizo cambiar la forma en que veo el desarrollo de software. Antes pensaba que entregar un proyecto era simplemente hacer que el código funcionara. Ahora entiendo que también incluye automatización, pruebas, seguridad, contenedores y poder seguir todo el proceso desde el desarrollo hasta el despliegue. Es probablemente una de las cosas más útiles que aprendí durante estas semanas.

---

## Uso de Inteligencia Artificial

De acuerdo con las políticas de uso ético de IA de Duoc UC, se declara el siguiente uso de herramientas de inteligencia artificial en este proyecto:

| Herramienta        | Uso aplicado                                                                      |
|--------------------|-----------------------------------------------------------------------------------|
| Claude (Anthropic) | Apoyo en la generación del README.md, revisión de estructura y redacción técnica  |

Todo el contenido generado con IA fue revisado y validado, asegurando coherencia con los requerimientos del proyecto y la pauta de evaluación. Las conclusiones personales fueron redactadas de forma individual, sin apoyo de IA, tal como lo exige la pauta.

> Referencia: https://bibliotecas.duoc.cl/ia

# 🚀 Microservicio Auth — Pipeline CI/CD

> **Evaluación Parcial N°2 · DOY0101 Ingeniería DevOps**  
> Automatización completa del ciclo de vida de un microservicio mediante GitHub Actions, Docker y Docker Compose.

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
- [Conclusiones Personales](#conclusiones-personales)
- [Uso de Inteligencia Artificial](#uso-de-inteligencia-artificial)

---

## Descripción del Proyecto

Este repositorio implementa un pipeline de **Integración y Entrega Continua (CI/CD)** para el microservicio de autenticación (`microservicio-auth`) desarrollado en la evaluación anterior. El objetivo es automatizar completamente el ciclo de vida del servicio: desde la compilación y pruebas hasta el análisis de seguridad, la construcción de imagen Docker y el despliegue en un entorno simulado.

El pipeline se ejecuta en **GitHub Actions** y cubre las siguientes etapas en orden:

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
| Contenedor | Docker | Alpine |
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
│       ├── ci-cd.yml          # Pipeline principal CI/CD (rama main)
│       └── cy.yml             # Pipeline básico de validación (develop/main)
├── .mvn/
│   └── wrapper/
│       └── maven-wrapper.properties
├── src/
│   ├── main/java/...          # Código fuente del microservicio
│   └── test/java/...          # Pruebas unitarias (JUnit)
├── Dockerfile                 # Imagen multi-stage del microservicio
├── docker-compose.yml         # Orquestación local del servicio
├── pom.xml                    # Dependencias y configuración Maven
└── README.md
```

---

## Arquitectura del Pipeline CI/CD

El pipeline principal (`ci-cd.yml`) se activa únicamente con **push a la rama `main`** y ejecuta los siguientes jobs en secuencia:

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

Los jobs de **SonarCloud** y **Snyk** corren en **paralelo** para optimizar el tiempo total del pipeline.

---

## Contenedorización (IE1)

El `Dockerfile` utiliza una estrategia **multi-stage build** para mantener la imagen final liviana y segura:

**Etapa 1 — Build:** usa `maven:3.9-eclipse-temurin-21-alpine` para compilar el proyecto y descargar dependencias. Se aprovecha el caché de capas de Docker copiando primero el `pom.xml` antes que el código fuente, lo que acelera compilaciones sucesivas cuando sólo cambia el código.

**Etapa 2 — Runtime:** parte desde `eclipse-temurin:21-jre-alpine` (solo JRE, sin Maven ni fuentes). Se crea un usuario sin privilegios (`appuser`) para no ejecutar el proceso como `root`, siguiendo buenas prácticas de seguridad en contenedores.

```dockerfile
FROM maven:3.9-eclipse-temurin-21-alpine AS build   # Solo para compilar
FROM eclipse-temurin:21-jre-alpine                  # Imagen final liviana
```

**Resultado:** imagen final significativamente más pequeña que una imagen con JDK completo, sin herramientas de build ni código fuente expuesto.

---

## Pruebas Automatizadas (IE2)

Las pruebas unitarias se ejecutan en el job `pruebas-unitarias`, que depende de que el `build` haya sido exitoso.

**Framework:** JUnit (integrado con Maven via `mvn test`)  
**Cobertura:** JaCoCo genera un reporte de cobertura de código que se sube como artefacto del pipeline para revisión posterior.

```yaml
- name: Ejecutar pruebas unitarias
  run: mvn test

- name: Subir reporte JaCoCo
  uses: actions/upload-artifact@v4
  with:
    name: jacoco-report
    path: target/site/jacoco/
```

El reporte JaCoCo queda disponible en la sección **Artifacts** de cada ejecución del workflow en GitHub, garantizando trazabilidad sobre el porcentaje de cobertura en cada merge a `main`.

---

## Seguridad y Calidad de Código (IE3)

Dos herramientas corren en paralelo tras las pruebas unitarias:

### SonarCloud
Analiza la calidad del código fuente: duplicaciones, code smells, bugs potenciales y cobertura de pruebas. El pipeline **bloquea el avance** si SonarCloud retorna un estado distinto a `passed` (Quality Gate), ya que el job `build-docker` tiene como dependencia (`needs`) tanto `security-sonar` como `security-snyk`.

### Snyk
Escanea las dependencias del proyecto en busca de vulnerabilidades conocidas (CVEs). Se configura con `--severity-threshold=high` para reportar solo vulnerabilidades de severidad alta o crítica. El reporte JSON se guarda como artefacto del pipeline.

### Dependabot
Configurado a nivel de repositorio en GitHub para revisar y proponer actualizaciones automáticas de dependencias Maven cuando se detectan versiones con vulnerabilidades conocidas.

**Mecanismo de bloqueo:** si cualquiera de los dos análisis de seguridad falla con error (no con `|| true`), el job `build-docker` no se ejecuta, impidiendo que una imagen comprometida llegue a Docker Hub o al entorno simulado.

---

## Despliegue Automatizado (IE4)

El job `despliegue-simulado` levanta el microservicio usando **Docker Compose** dentro del runner de GitHub Actions, simulando un entorno de producción:

1. Se instala Docker Compose en el runner
2. Se autentica en Docker Hub para acceder a la imagen recién construida
3. Se ejecuta `docker-compose up -d` para levantar el contenedor
4. Se verifica la disponibilidad del servicio consultando el endpoint `/actuator/health` (Spring Boot Actuator)
5. Se derriba el entorno con `docker-compose down` al finalizar

```bash
curl --fail http://localhost:8080/actuator/health || exit 1
```

Si el servicio no responde correctamente, el pipeline falla y se notifica al equipo, garantizando que solo versiones funcionales lleguen a producción.

---

## Orquestación de Contenedores (IE5)

El archivo `docker-compose.yml` define la orquestación del microservicio con las siguientes características:

**Política de reinicio:** `restart: always` asegura que el contenedor se relance automáticamente ante caídas inesperadas.

**Healthcheck:** verifica el estado del servicio cada 10 segundos con un timeout de 5 segundos y hasta 5 reintentos antes de marcar el contenedor como `unhealthy`:

```yaml
healthcheck:
  test: ["CMD-SHELL", "wget --quiet --tries=1 --spider http://localhost:8080/auth/health || exit 1"]
  interval: 10s
  timeout: 5s
  retries: 5
  start_period: 30s
```

**Inyección de variables de entorno:** las configuraciones sensibles (como credenciales de base de datos) se inyectan desde el entorno del host, nunca desde un archivo `.env` versionado, siguiendo buenas prácticas de seguridad (12-factor app).

**Escalabilidad:** la definición en Docker Compose permite en el futuro escalar el servicio horizontalmente con `docker-compose up --scale microservicio-auth=3`, o bien migrar la misma definición a un manifiesto de Kubernetes (`Deployment` + `Service`) para entornos de mayor envergadura.

---

## Trazabilidad y Calidad

La trazabilidad del pipeline se garantiza mediante los siguientes mecanismos:

| Mecanismo | Descripción |
|---|---|
| **SHA de commit en imagen Docker** | Cada imagen publicada en Docker Hub lleva el tag `latest` y además el SHA del commit (`github.sha`), permitiendo identificar exactamente qué versión del código está corriendo |
| **Artefactos de pipeline** | El reporte JaCoCo y el reporte Snyk quedan adjuntos a cada ejecución del workflow, con retención histórica |
| **Dependencia entre jobs** | El campo `needs` en cada job define el orden estricto de ejecución, haciendo imposible saltarse etapas |
| **Branch protection** | El pipeline solo se activa en `main`, lo que combinado con reglas de protección de rama obliga a que todo cambio pase por Pull Request y revisión |
| **Logs de GitHub Actions** | Cada paso del pipeline queda registrado con timestamps y output completo en la interfaz de GitHub |

---

## Secrets Requeridos

Para que el pipeline funcione correctamente, se deben configurar los siguientes **Repository Secrets** en GitHub (`Settings > Secrets and variables > Actions`):

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

### Pre-requisitos
- Docker Desktop instalado y en ejecución
- Java 21 (opcional, solo para compilar sin Docker)

### Levantar el servicio con Docker Compose

```bash
# 1. Clonar el repositorio
git clone <url-del-repositorio>
cd gitflow-encargo

# 2. Construir y levantar
docker-compose up --build

# 3. Verificar que el servicio responde
curl http://localhost:8080/auth/health

# 4. Detener el servicio
docker-compose down
```

### Ejecutar pruebas unitarias localmente

```bash
mvn test
```

### Ver reporte de cobertura

```bash
mvn test jacoco:report
# El reporte queda en: target/site/jacoco/index.html
```

---

## Conclusiones Personales

Durante el desarrollo de esta evaluación trabajé de forma individual, así que tuve que hacerme cargo de todo el proceso, desde armar el pipeline hasta configurar cada herramienta que se integraba. Fue complicado en varios momentos, pero también me sirvió para entender mejor cómo funciona realmente un ciclo CI/CD completo.

De las partes que mes me costaron fue aprender a leer los errores de Quality Gate de SonarCloud, que al comienzo me daba error el pipeline sin que yo entendiera bien por qué. Una vez que lo resolví, entendí que ese error no es un problema sino exactamente el objetivo, si hay problemas reales que se identifiquen y se corrijan.

También me costó bastante verificar que los secrets estuvieran funcionando correctamente. No era fácil saber si realmente se estaban usando o si el pipeline simplemente estaba fallando en silencio. Tuve que aprender a leer los logs de GitHub Actions y reconocer que los *** en el output confirmaban que el secret había sido inyectado correctamente, lo que me dio más confianza en que el pipeline estaba operando de forma segura.

Este proyecto me hizo cambiar la forma en que veo el desarrollo de software. Antes pensaba que entregar un proyecto era simplemente hacer que el código funcionara. Ahora entiendo que también incluye automatización, pruebas, seguridad, contenedores y poder seguir todo el proceso desde el desarrollo hasta el despliegue. Es probablemente una de las cosas más útiles que aprendí durante estas semanas.

---

## Uso de Inteligencia Artificial

De acuerdo con las políticas de uso ético de IA de Duoc UC, se declara el siguiente uso de herramientas de inteligencia artificial en este proyecto:

| Herramienta | Uso aplicado |
|---|---|
| Claude (Anthropic) | Apoyo en la generación del README.md, revisión de estructura y redacción técnica |

Todo el contenido generado con IA fue revisado y validado, asegurando coherencia con los requerimientos del proyecto y la pauta de evaluación.

Las conclusiones individuales de cada integrante fueron redactadas de forma personal, sin apoyo de IA, tal como lo exige la pauta.

> Referencia: https://bibliotecas.duoc.cl/ia

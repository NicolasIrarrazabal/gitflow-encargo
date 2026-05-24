# Microservicio de Autenticacion

Microservicio REST con Spring Boot que permite registrar e iniciar sesion con usuarios. Forma parte del Encargo 2 de Ingenieria DevOps.

## Tecnologias usadas

- Java 17 + Spring Boot 3.2.5
- JUnit 5 + Mockito + JaCoCo
- Docker + Docker Compose
- GitHub Actions
- Snyk + Dependabot
- Nginx

## Endpoints

### POST /api/auth/registro
Registra un usuario nuevo.

Body:
```json
{
  "username": "juan",
  "password": "mipass123"
}
```

Respuestas:
- `201` Usuario registrado correctamente
- `409` El usuario ya existe
- `400` Datos invalidos

### POST /api/auth/login
Autentica un usuario existente.

Body:
```json
{
  "username": "juan",
  "password": "mipass123"
}
```

Respuestas:
- `200` Login exitoso
- `401` Credenciales invalidas

### GET /api/auth/health
Verifica que el servicio este funcionando.

## Como ejecutar

### Con Docker Compose

```bash
docker compose up -d --build
```

Esto levanta el microservicio en el puerto 8080 y Nginx en el puerto 80.

### Solo la aplicacion

```bash
docker build -t microservicio-auth .
docker run -p 8080:8080 microservicio-auth
```

### Tests

```bash
mvn clean test
```

## Pipeline CI/CD

El pipeline se activa con cada push o pull request a `main` y `develop`. Tiene 4 etapas en orden:

**1. test** — Ejecuta los tests con JUnit y verifica cobertura minima del 70% con JaCoCo.

**2. security** — Escanea dependencias e imagen Docker con Snyk. Si encuentra vulnerabilidades HIGH o CRITICAL, bloquea el pipeline y no continua.

**3. build** — Construye la imagen Docker con multi-stage build y la sube a GitHub Container Registry con el tag del commit.

**4. deploy** — Despliega con Docker Compose, verifica el health check y registra los datos del despliegue (rama, commit, autor, fecha) en el GitHub Step Summary para trazabilidad.

## Configuracion de secrets

Agregar en Settings → Secrets → Actions:

| Secret | Descripcion |
|--------|-------------|
| `SNYK_TOKEN` | Token de API de Snyk (se obtiene en snyk.io → Account Settings) |

El `GITHUB_TOKEN` lo provee GitHub automaticamente.

## Trazabilidad

Cada despliegue queda registrado en el GitHub Step Summary con la rama, commit SHA, autor y fecha. Esto permite saber exactamente que version esta en cada entorno.

## Uso de IA

Se utilizo Claude (Anthropic) como apoyo en la generacion de estructura base del proyecto, Dockerfile y configuracion del pipeline. Todo el contenido fue revisado y ajustado por el equipo. Las conclusiones y justificaciones tecnicas son propias.

Referencia: https://bibliotecas.duoc.cl/ia

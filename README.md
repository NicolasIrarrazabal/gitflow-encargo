# Microservicio de Autenticación

Proyecto desarrollado de forma individual para la asignatura **DOY0101 Ingeniería DevOps** en Duoc UC. Consiste en un microservicio REST construido con Spring Boot que implementa autenticación básica de usuarios, contenerizado con Docker y automatizado mediante un pipeline CI/CD en GitHub Actions.

---

## Funcionalidad

El microservicio expone dos endpoints:

- `POST /auth/login` — Recibe usuario y contraseña. Si las credenciales son válidas, devuelve un token de autenticación.
- `GET /auth/health` — Verifica que el servicio esté funcionando correctamente.

---

## Tecnologías utilizadas

| Herramienta | Uso |
|---|---|
| Java 21 + Spring Boot 3.2 | Desarrollo del microservicio |
| Maven | Gestión de dependencias y build |
| JUnit 5 + Mockito | Pruebas unitarias |
| JaCoCo | Medición de cobertura de código |
| Docker + Docker Compose | Contenerización y orquestación |
| nginx | Reverse proxy |
| GitHub Actions | Pipeline CI/CD |
| Snyk | Análisis de vulnerabilidades |
| Dependabot | Actualización automática de dependencias |

---

## Ejecución local

### Con Docker Compose (recomendado)

```bash
docker compose up --build
```

El servicio queda disponible en `http://localhost:80`. Las solicitudes pasan primero por nginx, que actúa como reverse proxy hacia el microservicio. Elegí esta configuración para no exponer directamente el puerto del servicio y poder escalar instancias sin cambiar nada en el cliente.

### Sin Docker

```bash
mvn spring-boot:run
```

Responde directamente en `http://localhost:8080`.

---

## Pipeline CI/CD

El workflow se activa automáticamente con cada `push` o `pull request` hacia las ramas `main` y `develop`. El flujo sigue este orden:

```
Build → Tests → Seguridad → Docker → Despliegue Simulado
```

Cada etapa depende de la anterior (`needs:` en GitHub Actions), lo que garantiza que no se despliega código que no compile, no pase pruebas o tenga vulnerabilidades conocidas.

### 1. Build

Compila el proyecto con Maven para detectar errores temprano, antes de invertir tiempo en etapas posteriores.

### 2. Pruebas unitarias

```bash
mvn clean verify
```

JaCoCo mide la cobertura con los siguientes mínimos configurados:

- **70%** cobertura de líneas
- **60%** cobertura de ramas

Si no se alcanzan estos umbrales, el pipeline falla. Los reportes quedan almacenados como artefactos en GitHub Actions para revisión posterior.

### 3. Análisis de seguridad

Snyk analiza las dependencias del proyecto en busca de vulnerabilidades. Si detecta alguna de severidad **alta o crítica**, el workflow se detiene.

Además, Dependabot genera pull requests automáticos cuando hay actualizaciones pendientes en:

- Dependencias Maven
- Imágenes Docker
- GitHub Actions

### 4. Construcción de imagen Docker

Se genera una imagen usando el `Dockerfile` del proyecto, identificada con el SHA del commit para mantener trazabilidad entre el código y lo que se despliega.

### 5. Despliegue simulado

Como validación final del pipeline:

1. Se descargan los artefactos generados
2. Se levantan dos instancias del servicio con Docker Compose
3. Se verifica con `curl` que `/auth/health` responda correctamente
4. Se eliminan los contenedores

---

## Configuración de Snyk

Para que el análisis de seguridad funcione, es necesario agregar el token de Snyk como secret en el repositorio:

1. Crear cuenta en [snyk.io](https://snyk.io)
2. Copiar el token desde la configuración personal
3. En GitHub: `Settings → Secrets → Actions`
4. Crear el secret con el nombre `SNYK_TOKEN`

---

## Escalamiento

Docker Compose permite levantar múltiples instancias del microservicio con un solo comando:

```bash
docker compose up --build --scale microservicio-auth=2
```

También se configuraron healthchecks automáticos, reinicio ante fallos y límites básicos de CPU y memoria. Esto facilita agregar servicios adicionales en el futuro, como una base de datos o nuevos microservicios.

---

## Estructura del proyecto

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
│   └── dependabot.yml
├── nginx/
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

---

## Uso de Inteligencia Artificial

Usé IA como apoyo puntual durante el desarrollo, principalmente para:

- Corregir errores en pruebas unitarias
- Revisar sintaxis en los workflows de GitHub Actions
- Configurar Snyk y Dependabot por primera vez

La estructura del pipeline, las decisiones técnicas y el desarrollo del proyecto son propios.

> Referencia institucional: [Bibliotecas Duoc UC – Uso de IA](https://bibliotecas.duoc.cl)

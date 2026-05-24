# Microservicio de Autenticación

Microservicio REST desarrollado en Spring Boot para gestionar la autenticación de usuarios. Este proyecto fue contenerizado y automatizado como parte del encargo de la asignatura DOY0101 Ingeniería DevOps.

## Descripción

El microservicio expone dos endpoints:
- `POST /auth/login` recibe un usuario y contraseña, y si son válidos devuelve un token.
- `GET /auth/health` indica si el servicio está activo.

## Tecnologías usadas

- Java 17
- Spring Boot 3.2
- Maven
- JUnit 5 + Mockito
- JaCoCo (cobertura de código)
- Docker
- Docker Compose
- GitHub Actions
- Snyk (análisis de seguridad)
- Dependabot (actualización de dependencias)

## Cómo ejecutar localmente

### Con Docker Compose

```bash
docker compose up --build
```

El servicio queda disponible en `http://localhost:80` a través de nginx.

### Sin Docker

```bash
mvn spring-boot:run
```

## Pipeline CI/CD

El pipeline está implementado en GitHub Actions y se ejecuta automáticamente en cada push o pull request a las ramas `main` y `develop`.

### Etapas del pipeline

```
Build → Pruebas Unitarias → Análisis de Seguridad → Build Docker → Despliegue Simulado
```

**1. Build**
Compila el proyecto con Maven para verificar que no haya errores de compilación.

**2. Pruebas Unitarias**
Ejecuta los tests con JUnit usando `mvn clean verify`. Genera un reporte de cobertura con JaCoCo que queda disponible como artefacto del workflow.

**3. Análisis de Seguridad**
Usa Snyk para escanear las dependencias del proyecto. Si encuentra vulnerabilidades de severidad alta o crítica, el pipeline se bloquea y no continúa al siguiente paso. También está configurado Dependabot para abrir pull requests automáticos cuando hay dependencias desactualizadas.

**4. Build Docker**
Construye la imagen Docker del microservicio usando el Dockerfile del proyecto y la guarda como artefacto.

**5. Despliegue Simulado**
Descarga la imagen generada en el paso anterior, la levanta con Docker Compose con 2 réplicas y verifica que el endpoint `/auth/health` responda correctamente a través de nginx. Si la verificación falla, el pipeline también falla.

### Trazabilidad

Cada etapa del pipeline depende de la anterior (`needs:`), por lo que si alguna falla, las siguientes no se ejecutan. Esto garantiza que nunca se despliega código que no compiló, que no pasó los tests o que tiene vulnerabilidades conocidas.

Los artefactos generados (reporte JaCoCo e imagen Docker) quedan guardados en GitHub Actions para poder revisarlos después de cada ejecución.

### Configuración necesaria

Para que el job de seguridad funcione hay que agregar un secret en el repositorio:

1. Crear una cuenta gratuita en [snyk.io](https://snyk.io)
2. Copiar el token desde la configuración de la cuenta
3. Ir a Settings → Secrets → Actions en el repositorio de GitHub
4. Agregar el secret con el nombre `SNYK_TOKEN`

## Estructura del proyecto

```
├── src/
│   ├── main/java/com/encargo/microservicio/
│   │   ├── controller/AuthController.java
│   │   ├── model/Usuario.java
│   │   ├── service/AuthService.java
│   │   └── MicroservicioApplication.java
│   └── test/java/com/encargo/microservicio/
│       ├── AuthControllerTest.java
│       └── AuthServiceTest.java
├── .github/
│   ├── workflows/ci-cd.yml
│   └── dependabot.yml
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

## Orquestación con Docker Compose

El archivo `docker-compose.yml` define el servicio con un healthcheck que verifica cada 30 segundos que la app esté respondiendo. La red `app-network` permite agregar más servicios en el futuro (base de datos, frontend, etc.) sin exponerlos directamente al exterior.

El microservicio puede escalarse horizontalmente sin cambios adicionales, ya que el tráfico entra únicamente a través de nginx:

```bash
docker compose up --build --scale microservicio-auth=2
```

nginx balancea el tráfico entre réplicas usando round-robin sobre `app-network`.

## Uso de Inteligencia Artificial

Durante el desarrollo de este proyecto se utilizó Claude (Anthropic) como apoyo para:
- Corrección de errores de compilación en los tests
- Revisión de la sintaxis del archivo de GitHub Actions
- Consultas sobre la configuración de Snyk y Dependabot

Todas las decisiones de diseño, la estructura del proyecto y la lógica de autenticación fueron desarrolladas y validadas por el equipo. Las reflexiones personales y justificaciones técnicas son propias.

Referencia de citación IA: https://bibliotecas.duoc.cl/ia

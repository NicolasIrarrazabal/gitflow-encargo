# gitflow-encargo

Repositorio para practicar GitFlow. Se simula un flujo colaborativo con features y hotfixes usando Pull Requests.

## Por que GitFlow

Separa lo que está en producción de lo que se está desarrollando. Permite trabajar en features por separado y mergear cuando está listo, lo que tiene más sentido para trabajo en equipo que trunk-based donde todo va a una sola rama.

## Ramas

- **main** → producción
- **develop** → integración del desarrollo
- **feature/*** → funcionalidades nuevas, salen de develop
- **hotfix/*** → arreglos urgentes, salen de main

## Commits
feat: descripción corta
fix: descripción
hotfix: arreglo urgente

## Naming de ramas

- `feature/nombre-funcionalidad`
- `hotfix/descripcion-problema`

## Merge y Pull Requests

Nunca se pushea directo a main o develop. Todo pasa por PR, se revisa que no haya conflictos y se mergea. Los hotfixes van a main y también a develop para no perder el arreglo.

# 🛡️ Branch Protection Rules — Evaluación Parcial N°3 (IE5)

Este documento describe las **reglas de protección de rama** configuradas en GitHub
para el repositorio `gitflow-encargo`. Estas reglas complementan las herramientas
automatizadas (SonarCloud, Snyk, JaCoCo, audit-compliance.sh) para garantizar que
ningún cambio que incumpla los estándares de calidad llegue a producción.

---

## 📍 Configuración

Las reglas se aplican en:
**GitHub → Settings → Branches → Branch protection rules → `main`**

---

## ✅ Reglas habilitadas

### 1. Require a pull request before merging
- ✅ Activado
- **Justificación:** Ningún commit puede llegar directamente a `main`. Todo cambio
  debe pasar por un PR que sea revisado y aprobado.

### 2. Require approvals
- ✅ Activado — mínimo **1 reviewer**
- **Code Owners:** la lista en `.github/CODEOWNERS` asigna automáticamente al
  owner cuando se modifican archivos críticos (`.github/workflows/`,
  `observability/`, `src/main/`, `Dockerfile`/`docker-compose.yml`).

### 3. Dismiss stale pull request approvals when new commits are pushed
- ✅ Activado
- **Justificación:** Si el autor hace push de nuevos commits, las aprobaciones
  anteriores se invalidan, obligando a una re-revisión.

### 4. Require status checks to pass before merging
- ✅ Activado
- **Checks requeridos (todos deben pasar):**
  | Check name | Descripción |
  |---|---|
  | `build` | Compilación Maven del proyecto |
  | `pruebas-unitarias` | Tests unitarios JUnit + cobertura JaCoCo ≥70% |
  | `security-sonar` | SonarCloud Quality Gate = passed |
  | `security-snyk` | Snyk sin vulnerabilidades high/critical |
  | `compliance-audit` | Script de auditoría sin hallazgos críticos |
  | `validation-gate` | Gate final que valida todas las métricas anteriores |

### 5. Require linear history
- ✅ Activado
- **Justificación:** Evita merge commits ruidosos. Solo rebase o squash merge.

### 6. Include administrators
- ✅ Activado
- **Justificación:** Los administradores también deben cumplir las reglas,
  no pueden saltarse los checks (evita "privilege escalation").

### 7. Restrict who can push to matching branches
- ✅ Activado — solo `NicolasIrarrazabal` (admin) y el bot de GitHub Actions
- **Justificación:** Impide que contribuidores externos hagan push directo.

### 8. Allow force pushes
- ❌ Desactivado
- **Justificación:** Mantiene la historia inmutable y trazable.

### 9. Allow deletions
- ❌ Desactivado
- **Justificación:** Evita que la rama `main` se elimine accidentalmente.

---

## ☁️ Nota sobre el entorno de despliegue (IE2)

El despliegue orquestado del microservicio se realiza en una instancia
**AWS EC2**, gestionada vía SSH desde el job `deploy-ec2` del pipeline
(`.github/workflows/ci-cd.yml`), incluyendo verificación de salud del
contenedor y rollback automático si no queda `healthy` tras el despliegue.

---

```
       Push a feature branch
                ↓
       Pull Request a main
                ↓
   ┌──────────┴──────────┐
   │ Code Owner Review   │ ← CODEOWNERS asigna reviewer automático
   └──────────┬──────────┘
              ↓
   ┌──────────┴──────────────────────┐
   │ Status checks (todos required): │
   │   • build                       │
   │   • pruebas-unitarias           │
   │   • security-sonar              │
   │   • security-snyk               │
   │   • compliance-audit            │
   │   • validation-gate             │
   └──────────┬──────────────────────┘
              ↓
   ┌──────────┴──────────┐
   │ Aprobación mínima   │
   │ + linear history    │
   └──────────┬──────────┘
              ↓
       Squash and merge
              ↓
       CI/CD Pipeline corre
       (build → tests → deploy EC2)
```

---

## 🛠️ Configuración via API (referencia)

Si necesitas replicar estas reglas via API de GitHub:

```bash
gh api \
  --method PUT \
  -H "Accept: application/vnd.github+json" \
  /repos/OWNER/REPO/branches/main/protection \
  -f required_status_checks='{"strict":true,"contexts":["build","pruebas-unitarias","security-sonar","security-snyk","compliance-audit","validation-gate"]}' \
  -f required_pull_request_reviews='{"dismiss_stale_reviews":true,"require_code_owner_reviews":true,"required_approving_review_count":1}' \
  -f enforce_admins=true \
  -f required_linear_history=true \
  -F restrictions='{"users":["NicolasIrarrazabal"]}'
```

---

## 📚 Referencias

- [GitHub Docs: About branch protection rules](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches)
- [GitHub Docs: CODEOWNERS](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-code-owners)
- [SonarCloud Quality Gates](https://docs.sonarcloud.io/improving/quality-gates/)

---

> **Nota:** Este archivo es **documentación**. Las reglas reales se aplican
> desde la configuración de GitHub y desde los workflows en `.github/workflows/`.
> Si necesitas cambiar alguna regla, edita este archivo y aplícala vía UI o API.
#!/bin/bash
# IE5 — Script de auditoría de cumplimiento
#
# Verifica 3 categorías clave de cumplimiento:
#   1. No secretos hardcoded (API keys, passwords, tokens)
#   2. Configuración segura del contenedor de despliegue (Dockerfile/EC2)
#   3. Cobertura mínima de tests JaCoCo en pom.xml
#
# Exit codes:
#   0 = sin hallazgos críticos
#   1 = al menos un hallazgo crítico

set -e

REPO_ROOT="${1:-.}"
FAILURES=0

echo "Auditoria de cumplimiento (IE5) - repo: $REPO_ROOT - $(date -u +"%Y-%m-%dT%H:%M:%SZ")"
echo ""

print_ok() { echo "  [OK] $1"; }
print_fail() { echo "  [FAIL] $1"; FAILURES=$((FAILURES+1)); }
print_info() { echo "  [INFO] $1"; }
section() { echo ""; echo "--- $1 ---"; }

# 1. Detección de secretos hardcoded
section "Secretos hardcoded"

SECRET_PATTERNS=(
    'AKIA[0-9A-Z]{16}'                     # AWS Access Key
    'aws_secret_access_key\s*='
    'password\s*=\s*["'"'"'][^"'"'"']{8,}'
    'api[_-]?key\s*=\s*["'"'"'][a-zA-Z0-9]{20,}'
    'ghp_[a-zA-Z0-9]{36}'                  # GitHub PAT
)

SECRETS_FOUND=0
for pattern in "${SECRET_PATTERNS[@]}"; do
    # Excluir docs, ejemplos, y archivos de configuración con placeholders
    HITS=$(grep -rEn "$pattern" \
        --include="*.java" --include="*.properties" --include="*.yml" \
        --include="*.yaml" --include="*.xml" \
        --exclude-dir=".git" --exclude-dir="target" --exclude-dir="logs" \
        --exclude-dir="docs" \
        "$REPO_ROOT" 2>/dev/null \
        | grep -v "example\|sample\|placeholder\|REPLACE_\|USERNAME\|SECRET_ACCESS_KEY" \
        | head -3)
    if [ -n "$HITS" ]; then
        echo "$HITS"
        SECRETS_FOUND=1
    fi
done

if [ "$SECRETS_FOUND" -eq 0 ]; then
    print_ok "No se detectaron secretos hardcoded en código/configs"
else
    print_fail "Posibles secretos hardcodeados detectados (revisar)"
fi

# 2. Validación de configuración segura de despliegue (Dockerfile/EC2)
section "Configuracion segura de despliegue (Dockerfile/EC2)"

DOCKERFILE="$REPO_ROOT/Dockerfile"
if [ -f "$DOCKERFILE" ]; then
    if grep -q "^USER " "$DOCKERFILE"; then
        print_ok "Dockerfile define un usuario sin privilegios (no root)"
    else
        print_fail "Dockerfile no define USER no-root para el contenedor"
    fi

    if grep -q "^HEALTHCHECK" "$DOCKERFILE"; then
        print_ok "Dockerfile define HEALTHCHECK para el despliegue en EC2"
    else
        print_fail "Dockerfile no define HEALTHCHECK"
    fi
else
    print_info "No se encontró Dockerfile"
fi

# Verificar que el job deploy-ec2 no use credenciales hardcoded, solo secrets
EC2_WORKFLOW="$REPO_ROOT/.github/workflows/ci-cd.yml"
if [ -f "$EC2_WORKFLOW" ] && grep -q "deploy-ec2" "$EC2_WORKFLOW"; then
    if grep -q "secrets.EC2_HOST" "$EC2_WORKFLOW" && grep -q "secrets.EC2_SSH_KEY" "$EC2_WORKFLOW"; then
        print_ok "Despliegue a EC2 usa GitHub Secrets (host/credenciales no hardcoded)"
    else
        print_fail "Despliegue a EC2 no referencia secrets para host/credenciales"
    fi
else
    print_info "No se encontró job deploy-ec2 en el workflow"
fi

# 3. Verificar JaCoCo en pom.xml
section "Cobertura JaCoCo en pom.xml"

POM_FILE="$REPO_ROOT/pom.xml"
if [ -f "$POM_FILE" ]; then
    if grep -q "jacoco-maven-plugin" "$POM_FILE"; then
        print_ok "JaCoCo está configurado en pom.xml"

        # Verificar umbral ≥70%
        if grep -A 3 "<limit>" "$POM_FILE" | grep -E "<minimum>0\.[7-9]" > /dev/null; then
            print_ok "Umbral de cobertura ≥70% configurado"
        else
            print_fail "Umbral de cobertura <70% (configurar mínimo 0.70)"
        fi
    else
        print_fail "JaCoCo no está configurado en pom.xml"
    fi
else
    print_info "No se encontró pom.xml"
fi

echo ""
echo "Resumen: $FAILURES fallo(s) critico(s)"

if [ "$FAILURES" -gt 0 ]; then
    echo "Auditoria fallida, el pipeline se detiene aqui."
    exit 1
fi

echo "Auditoria OK, el pipeline puede continuar."
exit 0
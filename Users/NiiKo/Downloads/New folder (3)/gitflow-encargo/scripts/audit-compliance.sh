#!/bin/bash
# ============================================================
# IE5 — Script de auditoría de cumplimiento
# ============================================================
#
# Verifica 3 categorías clave de cumplimiento:
#   1. No secretos hardcoded (API keys, passwords, tokens)
#   2. Manifiestos Kubernetes válidos (estructura + security)
#   3. Cobertura mínima de tests JaCoCo en pom.xml
#
# Exit codes:
#   0 = sin hallazgos críticos
#   1 = al menos un hallazgo crítico

set -e

REPO_ROOT="${1:-.}"
FAILURES=0

echo "============================================================"
echo "  IE5 — Auditoría de Cumplimiento"
echo "  Repo: $REPO_ROOT"
echo "  Fecha: $(date -u +"%Y-%m-%dT%H:%M:%SZ")"
echo "============================================================"
echo ""

# ============================================================
# Helpers
# ============================================================
print_ok()    { echo "  [OK]      $1"; }
print_fail()  { echo "  [FAIL]    $1"; FAILURES=$((FAILURES+1)); }
print_info()  { echo "  [INFO]    $1"; }
section()     { echo ""; echo "--- $1 ---"; }

# ============================================================
# 1. Detección de secretos hardcoded
# ============================================================
section "1. Detección de secretos hardcoded"

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

# ============================================================
# 2. Validación de manifiestos Kubernetes
# ============================================================
section "2. Validación de manifiestos Kubernetes"

K8S_FILES=$(find "$REPO_ROOT/k8s" -name "*.yaml" 2>/dev/null)
if [ -n "$K8S_FILES" ]; then
    for f in $K8S_FILES; do
        if grep -q "^apiVersion:" "$f" && \
           grep -q "^kind:" "$f" && \
           grep -q "^metadata:" "$f"; then
            print_ok "$f tiene estructura K8s válida"
        else
            print_fail "$f no tiene estructura K8s válida"
        fi
    done

    # Verificar security context (no root)
    if grep -r "runAsNonRoot: true" "$REPO_ROOT/k8s" 2>/dev/null | head -1; then
        print_ok "Pods configuran runAsNonRoot: true"
    else
        print_fail "No se encontró runAsNonRoot: true en los manifests"
    fi
else
    print_info "No se encontraron manifiestos K8s"
fi

# ============================================================
# 3. Verificar JaCoCo en pom.xml
# ============================================================
section "3. Cobertura JaCoCo en pom.xml"

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

# ============================================================
# Resumen final
# ============================================================
echo ""
echo "============================================================"
echo "  RESUMEN DE AUDITORÍA"
echo "============================================================"
echo "  Fallos críticos:  $FAILURES"
echo "============================================================"

if [ "$FAILURES" -gt 0 ]; then
    echo ""
    echo "❌ AUDITORÍA FALLÓ — el pipeline debe detenerse"
    exit 1
fi

echo ""
echo "✅ AUDITORÍA EXITOSA — pipeline puede continuar"
exit 0
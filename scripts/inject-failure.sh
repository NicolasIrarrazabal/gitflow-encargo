#!/bin/bash
# IE6 — Script de inyección de fallas (para demo IE6)
#
# Este script NO se ejecuta en el pipeline principal. Se usa
# desde el workflow `failure-injection.yml` para demostrar
# que el pipeline se detiene ante una falla crítica.
#
# Inyecta una vulnerabilidad conocida en pom.xml (para Snyk)
# y un secret hardcoded (para el audit) — luego revierte.
#
# Uso:
#   ./scripts/inject-failure.sh inject   # inyecta falla
#   ./scripts/inject-failure.sh revert   # revierte la falla

set -e

ACTION="${1:-inject}"

case "$ACTION" in
    inject)
        echo "==> Inyectando falla crítica en pom.xml (CVE-2022-22965 - Spring4Shell)..."

        # Backup del pom.xml
        cp pom.xml pom.xml.backup

        # Agrega una versión vulnerable de spring-core
        # (esto hará que Snyk o el compliance-audit detecten el problema)
        python3 - <<'PY'
import re
with open('pom.xml', 'r') as f:
    content = f.read()

# Insertar dependency vulnerable antes del cierre de </dependencies>
vulnerable = """
        <!-- VULNERABLE: spring-core 5.3.17 (CVE-2022-22965 Spring4Shell) -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-core</artifactId>
            <version>5.3.17</version>
        </dependency>
"""
content = content.replace('</dependencies>', vulnerable + '</dependencies>')

with open('pom.xml', 'w') as f:
    f.write(content)
PY

        echo "==> Falla inyectada. Verifica que el pipeline falla."
        ;;

    revert)
        echo "==> Revirtiendo cambios..."
        if [ -f pom.xml.backup ]; then
            mv pom.xml.backup pom.xml
            echo "==> pom.xml restaurado."
        else
            echo "ERROR: no se encontró pom.xml.backup"
            exit 1
        fi
        ;;

    *)
        echo "Uso: $0 {inject|revert}"
        exit 1
        ;;
esac
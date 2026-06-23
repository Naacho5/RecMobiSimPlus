#!/bin/bash

export ANT_OPTS="-Dfile.encoding=iso-8859-1"

cd .. || {
  echo "[ERROR] No se pudo acceder al directorio raiz del proyecto."
  echo "        Comprueba la ubicacion del script."
  exit 1
}

export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"

echo "============================================================"
echo "  RecMobiSim - Compilacion del proyecto"
echo "============================================================"
echo
echo "[INFO] Directorio del proyecto: $(pwd)"
echo "[INFO] JAVA_HOME: $JAVA_HOME"
echo

if [ ! -x "$JAVA_HOME/bin/java" ]; then
  echo "[ERROR] No se ha encontrado java en:"
  echo "        $JAVA_HOME/bin/java"
  exit 1
fi

if ! command -v ant >/dev/null 2>&1; then
  echo "[ERROR] Apache Ant no esta instalado o no es accesible desde PATH."
  exit 1
fi

if [ ! -f "build.xml" ]; then
  echo "[ERROR] No se ha encontrado build.xml en:"
  echo "        $(pwd)"
  exit 1
fi

echo "[INFO] Version de Java detectada:"
java -version || {
  echo "[ERROR] No se pudo ejecutar Java correctamente."
  exit 1
}

echo
echo "[INFO] Version de Ant detectada:"
ant -version || {
  echo "[ERROR] No se pudo ejecutar Ant correctamente."
  exit 1
}

echo
echo "=== Limpieza segura de cache de SQLite ==="
rm -rf /tmp/sqlite-* ~/.cache/sqlite-jdbc-* /tmp/libsqlitejdbc.so 2>/dev/null
echo "[INFO] Limpieza de temporales completada."

echo
echo "=== Eliminando directorio bin/ ==="
if [ -d "bin" ]; then
  rm -rf bin/ || {
    echo "[WARN] No se pudo eliminar completamente bin/."
  }
  [ ! -d "bin" ] && echo "[OK] Directorio bin/ eliminado correctamente."
else
  echo "[INFO] El directorio bin/ no existe."
fi

echo
echo "=== Ejecutando Ant: clean ==="
ant clean || {
  echo "[ERROR] El target 'clean' ha fallado."
  exit 1
}

echo
echo "=== Ejecutando Ant: build ==="
ant build || {
  echo "[ERROR] El target 'build' ha fallado."
  exit 1
}

echo
echo "=== Ejecutando Ant: jar ==="
ant jar || {
  echo "[ERROR] El target 'jar' ha fallado."
  exit 1
}

echo
if [ -f "dist/RecMobiSim.jar" ]; then
  echo "[OK] Compilacion completada correctamente."
  echo "[OK] JAR generado en: $(pwd)/dist/RecMobiSim.jar"
else
  echo "[WARN] Compilacion completada, pero no se ha encontrado dist/RecMobiSim.jar"
  echo "       Revisa build.xml por si el JAR se genera en otra ruta."
fi


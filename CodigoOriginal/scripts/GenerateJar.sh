# export ANT_OPTS=" -Dfile.encoding=iso-8859-1"
# cd ..
# # Añadido nuevo
# export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
# export PATH=$JAVA_HOME/bin:$PATH
# ant build jar

#!/bin/bash
export ANT_OPTS="-Dfile.encoding=iso-8859-1"
cd ..

export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

echo "=== Limpiando caché de librerías nativas SQLite ==="
rm -rf /tmp/sqlite-*
rm -rf ~/.cache/sqlite-jdbc-*
rm -rf /tmp/libsqlitejdbc.so

echo "=== Eliminando bin/ con permisos elevados ==="
# sudo rm -rf bin/
rm -rf bin/

java -version || exit 1

ant clean
ant build
ant jar

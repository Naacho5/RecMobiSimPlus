export ANT_OPTS=" -Dfile.encoding=iso-8859-1"
cd ..

# Añadido nuevo
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

ant jar

#!/bin/sh
# ----------------------------------------------------------------------------
# Apache Maven Wrapper Executable Header
# ----------------------------------------------------------------------------

# Set environment
[ -z "$MAVEN_BASEDIR" ] && MAVEN_BASEDIR="$(pwd)"
PRG="$0"
while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done
saveddir=`pwd`
MAVEN_PROJECTBASEDIR=`dirname "$PRG"`
cd "$MAVEN_PROJECTBASEDIR"
MAVEN_PROJECTBASEDIR=`pwd`
cd "$saveddir"

# Locate wrapper jar or download
WRAPPER_DIR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper"
WRAPPER_JAR="$WRAPPER_DIR/maven-wrapper.jar"
WRAPPER_PROPERTIES="$WRAPPER_DIR/maven-wrapper.properties"

# Determine Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
else
    JAVACMD="java"
fi

if [ ! -x "$JAVACMD" ] ; then
    echo "Error: JAVA_HOME is not defined correctly." >&2
    echo "  We cannot execute $JAVACMD" >&2
    exit 1
fi

WRAPPER_LAUNCHER="org.apache.maven.wrapper.MavenWrapperMain"

# Download the wrapper jar if not present
if [ ! -f "$WRAPPER_JAR" ]; then
    WRAPPER_URL="https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
    if [ -f "$WRAPPER_PROPERTIES" ]; then
        WRAPPER_URL=$(sed -n 's/^wrapperUrl=\(.*\)$/\1/p' "$WRAPPER_PROPERTIES")
    fi
    mkdir -p "$WRAPPER_DIR"
    if which curl >/dev/null 2>&1; then
        curl -o "$WRAPPER_JAR" -fsSL "$WRAPPER_URL"
    elif which wget >/dev/null 2>&1; then
        wget -O "$WRAPPER_JAR" "$WRAPPER_URL"
    fi
fi

exec "$JAVACMD" \
    -Dmaven.home="$MAVEN_HOME" \
    -Dmaven.multiModuleProjectDirectory="$MAVEN_PROJECTBASEDIR" \
    -classpath "$WRAPPER_JAR" \
    "$WRAPPER_LAUNCHER" "$@"

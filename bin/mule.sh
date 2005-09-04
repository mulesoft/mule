#!/bin/sh

#   Mule shell script
#
#   $Id$
#
#
#   Copyright (c) 2005 The Apache Software Foundation.  All rights
#   reserved.

# load system-wide mule configuration
if [ -f "/etc/mule.conf" ] ; then
  . /etc/mule.conf
fi

# provide default values for people who don't use RPMs
if [ -z "$usejikes" ] ; then
  usejikes=false;
fi

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false;
darwin=false;
case "`uname`" in
  CYGWIN*) cygwin=true ;;
  Darwin*) darwin=true
           if [ -z "$JAVA_HOME" ] ; then
             JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Home
           fi
           ;;
esac

if [ -z "$MULE_HOME" ] ; then
  # try to find mule
  if [ -d /opt/mule ] ; then
    MULE_HOME=/opt/mule
  fi

  if [ -d "${HOME}/opt/mule" ] ; then
    MULE_HOME="${HOME}/opt/mule"
  fi
  
  # load user mule configuration
  if [ -f "$MULE_HOME/.mulerc" ] ; then
  . "$MULE_HOME/.mulerc"
  fi

  ## resolve links - $0 may be a link to mule's home
  PRG="$0"
  progname=`basename "$0"`
  saveddir=`pwd`

  # need this for relative symlinks
  dirname_prg=`dirname "$PRG"`
  cd "$dirname_prg"

  while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '.*/.*' > /dev/null; then
	PRG="$link"
    else
	PRG=`dirname "$PRG"`"/$link"
    fi
  done

  MULE_HOME=`dirname "$PRG"`/..

  cd "$saveddir"

  # make it fully qualified
  MULE_HOME=`cd "$MULE_HOME" && pwd`
fi

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
  [ -n "$MULE_HOME" ] &&
    MULE_HOME=`cygpath --unix "$MULE_HOME"`
  [ -n "$JAVA_HOME" ] &&
    JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
  [ -n "$CLASSPATH" ] &&
    CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
fi

# set MULE_LIB location
MULE_LIB="${MULE_HOME}/lib"

if [ -z "$JAVACMD" ] ; then
  if [ -n "$JAVA_HOME"  ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
      # IBM's JDK on AIX uses strange locations for the executables
      JAVACMD="$JAVA_HOME/jre/sh/java"
    else
      JAVACMD="$JAVA_HOME/bin/java"
    fi
  else
    JAVACMD=`which java 2> /dev/null `
    if [ -z "$JAVACMD" ] ; then
        JAVACMD=java
    fi
  fi
fi

if [ ! -x "$JAVACMD" ] ; then
  echo "Error: JAVA_HOME is not defined correctly."
  echo "  We cannot execute $JAVACMD"
  exit 1
fi

if [ -n "$CLASSPATH" ] ; then
  LOCALCLASSPATH="$CLASSPATH"
fi

# add in the core .jar files
for i in "${MULE_HOME}"/*.jar
do
  # if the directory is empty, then it will return the input string
  # this is stupid, so case for it
  if [ -f "$i" ] ; then
    if [ -z "$LOCALCLASSPATH" ] ; then
      LOCALCLASSPATH="$i"
    else
      LOCALCLASSPATH="$i":"$LOCALCLASSPATH"
    fi
  fi
done

# add in the required dependency .jar files
for i in "${MULE_LIB}"/*.jar
do
  # if the directory is empty, then it will return the input string
  # this is stupid, so case for it
  if [ -f "$i" ] ; then
    if [ -z "$LOCALCLASSPATH" ] ; then
      LOCALCLASSPATH="$i"
    else
      LOCALCLASSPATH="$i":"$LOCALCLASSPATH"
    fi
  fi
done

# add in the optional dependency .jar files
for i in "${MULE_LIB}"/opt/*.jar
do
  # if the directory is empty, then it will return the input string
  # this is stupid, so case for it
  if [ -f "$i" ] ; then
    if [ -z "$LOCALCLASSPATH" ] ; then
      LOCALCLASSPATH="$i"
    else
      LOCALCLASSPATH="$i":"$LOCALCLASSPATH"
    fi
  fi
done

LOCALCLASSPATH="${MULE_HOME}/conf:$LOCALCLASSPATH"

# For Cygwin, switch paths to Windows format before running java
if $cygwin; then
  MULE_HOME=`cygpath --windows "$MULE_HOME"`
  JAVA_HOME=`cygpath --windows "$JAVA_HOME"`
  CLASSPATH=`cygpath --path --windows "$CLASSPATH"`
  LOCALCLASSPATH=`cygpath --path --windows "$LOCALCLASSPATH"`
  CYGHOME=`cygpath --windows "$HOME"`
fi


if [ -n "MULE_OPTS" ] ; then
  MULE_OPTS="-Xmx512M"
fi

if [ -n "MULE_MAIN" ] ; then
  MULE_MAIN="org.mule.MuleServer"
fi

# Uncomment to enable YourKit profiling
#MULE_DEBUG_OPTS="-Xrunyjpagent"

# Uncomment to enable remote debugging
#MULE_DEBUG_OPTS="-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"

if [ -n "$CYGHOME" ]; then
    exec "$JAVACMD" $MULE_DEBUG_OPTS $MULE_OPTS -classpath "$LOCALCLASSPATH" -Dorg.mule.home="${MULE_HOME}" -Dcygwin.user.home="$CYGHOME" $MULE_MAIN $MULE_ARGS "$@"
else
    exec "$JAVACMD" $MULE_DEBUG_OPTS $MULE_OPTS -classpath "$LOCALCLASSPATH" -Dorg.mule.home="${MULE_HOME}" $MULE_MAIN $MULE_ARGS "$@"
fi


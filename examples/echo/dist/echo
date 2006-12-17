#! /bin/sh

# There is no need to call this if you set the MULE_HOME in your environment
if [ -z "$MULE_HOME" ] ; then
  MULE_HOME=../../..
  export MULE_HOME
fi

# If MULE_BASE is not set, make it MULE_HOME
if [ -z "$MULE_BASE" ] ; then
  MULE_BASE=$MULE_HOME
  export MULE_BASE
fi

# Any changes to the files in ./conf will take precedence over those deployed to $MULE_HOME/lib/user
MULE_LIB=./conf
export MULE_LIB

exec $MULE_BASE/bin/mule -config ./conf/echo-config.xml


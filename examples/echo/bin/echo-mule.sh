#! /bin/sh
# There is no need to call this if you set the MULE_HOME in your environment
if [ -z "$MULE_HOME" ] ; then
  MULE_HOME=../../..
  export MULE_HOME
fi

# Set your application specific classpath like this.
# The echo does not use any user-defined classes, but just in case
# you want to play with any
MULE_LIB=$MULE_HOME/examples/echo/classes
export MULE_LIB

$MULE_HOME/bin/mule -config ../conf/echo-config.xml


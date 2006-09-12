#! /bin/sh
# There is no need to call this if you set the MULE_HOME in your environment
if [ -z "$MULE_HOME" ] ; then
  MULE_HOME=../../..
  export MULE_HOME
fi

# Set your application specific classpath like this
MULE_LIB=$MULE_HOME/examples/hello/classes:$MULE_HOME/examples/hello/conf
export MULE_LIB

$MULE_HOME/bin/mule -config ../conf/hello-config.xml


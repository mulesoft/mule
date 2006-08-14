#! /bin/sh
# There is no need to call this if you set the MULE_HOME in your environment
if [ -z "$MULE_HOME" ] ; then
  MULE_HOME=../../..
  export MULE_HOME
fi

# Set your application specific classpath like this
CLASSPATH=$MULE_HOME/samples/stockquote/conf:$MULE_HOME/samples/stockquote/classes
export CLASSPATH

$MULE_HOME/bin/mule -config ../conf/wsdl-mule-config.xml

CLASSPATH=
export CLASSPATH

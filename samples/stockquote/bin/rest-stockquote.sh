#! /bin/sh
# There is no need to call this if you set the MULE_HOME in your environment
export MULE_HOME=../../..

# Set your application specific classpath like this
export CLASSPATH=$MULE_HOME/samples/stockquote/conf:$MULE_HOME/samples/stockquote/classes

$MULE_HOME/bin/mule -config ../conf/rest-mule-config.xml

export CLASSPATH=
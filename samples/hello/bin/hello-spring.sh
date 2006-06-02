#! /bin/sh
# There is no need to call this if you set the MULE_HOME in your environment
if [ -z "$MULE_HOME" ] ; then
  export MULE_HOME=../../..
fi

# Set your application specific classpath like this
export CLASSPATH=$MULE_HOME/samples/hello/conf:$MULE_HOME/samples/hello/classes

$MULE_HOME/bin/mule -config ../conf/hello-spring-config.xml -builder org.mule.extras.spring.config.SpringConfigurationBuilder

export CLASSPATH=
#! /bin/sh
# There is no need to call this if you set the MULE_HOME in your environment
exec ../../../bin/sethome

# Set your application specific classpath like this
export CLASSPATH=$MULE_HOME/samples/hello/conf:$MULE_HOME/samples/hello/classes

exec $MULE_HOME/bin/mule -config ../conf/hello-http-mule-config.xml

export CLASSPATH=
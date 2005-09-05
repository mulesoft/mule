#! /bin/sh
# There is no need to call this if you set the MULE_HOME in your environment
exec ../../../bin/sethome

# Set your application specific classpath like this
export CLASSPATH=$MULE_HOME/samples/echo/conf

exec $MULE_HOME/bin/mule -config ../conf/mule-config.xml

export CLASSPATH=
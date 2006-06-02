#! /bin/sh

# Set your application specific classpath like this
export CLASSPATH=conf:classes

$MULE_HOME/bin/mule -config conf/mule-config.xml

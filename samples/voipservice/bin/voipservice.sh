#! /bin/sh
# There is no need to call this if you set the MULE_HOME in your environment
if [ -z "$MULE_HOME" ] ; then
  export MULE_HOME=../../..
fi

#Set the main class to run
export MULE_MAIN=org.mule.samples.voipservice.client.VoipConsumer

# Set your application specific classpath like this
export CLASSPATH=$MULE_HOME/samples/voipservice/conf:$MULE_HOME/samples/voipservice/classes

$MULE_HOME/bin/mule
export MULE_MAIN=
export CLASSPATH=
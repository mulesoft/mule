#! /bin/sh
# There is no need to call this if you set the MULE_HOME in your environment
if [ -z "$MULE_HOME" ] ; then
  export MULE_HOME=../../..
fi

#Set the main class to run, this is not necessay if you are using org.mule.MuleServer
export MULE_MAIN=org.mule.samples.loanbroker.esb.Main

# Set your application specific classpath like this
export CLASSPATH=$MULE_HOME/samples/loanbroker-esb/conf:$MULE_HOME/samples/loanbroker-esb/classes
export CUSTOM_LIB=$MULE_HOME/samples/loanbroker-esb/lib

$MULE_HOME/bin/mule
export MULE_MAIN=
export CLASSPATH=
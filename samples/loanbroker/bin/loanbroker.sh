#! /bin/sh
# There is no need to call this if you set the MULE_HOME in your environment
if [ -z "$MULE_HOME" ] ; then
  MULE_HOME=../../..
  export MULE_HOME
fi

#Set the main class to run, this is not necessay if you are using org.mule.MuleServer
MULE_MAIN=org.mule.samples.loanbroker.LoanConsumer
export MULE_MAIN

# Set your application specific classpath like this
CLASSPATH=$MULE_HOME/samples/loanbroker/conf:$MULE_HOME/samples/loanbroker/classes
export CLASSPATH

$MULE_HOME/bin/mule

MULE_MAIN=
export MULE_MAIN
CLASSPATH=
export CLASSPATH

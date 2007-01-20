#! /bin/sh

# There is no need to call this if you set the MULE_HOME in your environment
if [ -z "$MULE_HOME" ] ; then
  MULE_HOME=../../..
  export MULE_HOME
fi

# If MULE_BASE is not set, make it MULE_HOME
if [ -z "$MULE_BASE" ] ; then
  MULE_BASE=$MULE_HOME
  export MULE_BASE
fi

# Any changes to the files in ./conf will take precedence over those deployed to $MULE_HOME/lib/user
MULE_LIB=./conf
export MULE_LIB
ACTIVEMQ=activemq-3.4.2.jar
OPENEJB=openejb-core-1.0.jar

if [ ! -f "$MULE_BASE/lib/user/${OPENEJB}" ]; then
   if [ ! -f "$MULE_HOME/lib/user/${OPENEJB}" ]; then
      MISSING_LIB=1
   fi
fi

if [ ! -f "$MULE_BASE/lib/user/${ACTIVEMQ}" ]; then
   if [ ! -f "$MULE_HOME/lib/user/${ACTIVEMQ}" ]; then
      MISSING_LIB=1
   fi
fi

if [ -z "$MISSING_LIB" ]
then
    exec $MULE_BASE/bin/mule -main org.mule.samples.loanbroker.esb.Main
else
    echo "This example requires additional libraries which need to be downloaded by the build script.  Please follow the instructions in the README.txt file."
fi

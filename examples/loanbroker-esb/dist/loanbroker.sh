#! /bin/sh

# There is no need to call this if you set the MULE_HOME in your environment
if [ -z "$MULE_HOME" ] ; then
  MULE_HOME=../../..
  export MULE_HOME
fi

# Any changes to the files in ./conf will take precedence over those deployed to $MULE_HOME/lib/user
MULE_LIB=./conf
export MULE_LIB

if [ ! -f "$MULE_HOME/lib/user/openejb.jar" ]
then
    MISSING_LIB=1
elif [ ! -f "$MULE_HOME/lib/user/activemq.jar" ]
then
    MISSING_LIB=1
fi

if [ -z "$MISSING_LIB" ]
then
    exec $MULE_HOME/bin/mule -main org.mule.samples.loanbroker.esb.Main
else
    echo "This example requires additional libraries which need to be downloaded by the build script.  Please follow the instructions in the README.txt file."
fi

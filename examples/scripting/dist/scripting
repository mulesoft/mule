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

# Check for additional libraries
if [ -f "$MULE_BASE/lib/user/groovy.jar" ]; then
        exec $MULE_BASE/bin/mule -config ./conf/scripting-config.xml
elif [ -f "$MULE_HOME/lib/user/groovy.jar" ]; then
        exec $MULE_BASE/bin/mule -config ./conf/scripting-config.xml
else
    echo "This example requires additional libraries which need to be downloaded by the build script.  Please follow the instructions in the README.txt file."
fi

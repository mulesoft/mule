#! /bin/sh

# There is no need to call this if you set the MULE_HOME in your environment
if [ -z "$MULE_HOME" ] ; then
  MULE_HOME=../../..
  export MULE_HOME
fi

# Any changes to the files in ./conf will take precedence over those deployed to $MULE_HOME/lib/user
MULE_LIB=./conf
export MULE_LIB

# Check for additional libraries
if [ ! -f "$MULE_HOME/lib/user/groovy.jar" ]
then
    echo "This example requires additional libraries which need to be downloaded by the build script.  Please follow the instructions in the README.txt file."
    exit 1
fi

echo "The Scripting example is available in two variations:"
echo "  1. Binary HTTP"
echo "  2. Text file"
echo "Select the one you wish to execute and press Enter..."
read i

if [ 1 = $i ]
then
    exec $MULE_HOME/bin/mule -main org.mule.samples.scripting.BinaryHttpExample
elif [ 2 = $i ]
then
    exec $MULE_HOME/bin/mule -main org.mule.samples.scripting.TextFileExample
fi

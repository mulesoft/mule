#! /bin/sh

# You can only call this script if MULE_HOME and MULE_BASE are defined
if [ -z "$MULE_HOME" ] ; then
  echo "You must first set the MULE_HOME environment variable"
  echo "to point to your Mule installation directory."
  exit -1
fi

if [ -z "$MULE_BASE" ] ; then
  echo "You must first set the MULE_BASE environment variable"
  echo "to point to the local directory from which you want to"
  echo "run Mule."
  exit -1
fi

echo "Creating Mule directories ..."

mkdir $MULE_BASE
mkdir $MULE_BASE/lib
mkdir $MULE_BASE/lib/user
mkdir $MULE_BASE/logs

echo "Copying Mule files ..."

cp -r $MULE_HOME/bin $MULE_BASE/
cp -r $MULE_HOME/conf $MULE_BASE/

if [ ! -z "$MULE_HOME/examples" ]; then
   echo "Do you want to copy the examples directories to your local"
   echo "Mule installation directory [y/n]?"
   read i
   if [ 'y' = $i ]; then
      echo "Copying example files ..."
      cp -r $MULE_HOME/examples $MULE_BASE/
   elif [ 'Y' = $i ]; then
      echo "Copying example files ..."
      cp -r $MULE_HOME/examples $MULE_BASE/
   fi
fi

echo "All done!"

#! /bin/sh

# Test script for MULE-1152 (java service wrapper w/ support for multiple users/instances)
#
# 1. Place this script in $MULE_HOME/bin 
# 2. Place the dummy mule-config.xml in $MULE_HOME/conf/mule-config.xml
# 3. Run this script and verify the expected output visually.

# Start two Mule apps
MULE_APP=mule1
export MULE_APP
mule start
MULE_APP=mule2
export MULE_APP
mule start

sleep 5

# Make sure both apps are running
MULE_APP=mule1
export MULE_APP
echo "mule1 should be running"
mule status
MULE_APP=mule2
export MULE_APP
echo "mule2 should be running"
mule status

sleep 1

# Stop the first app
MULE_APP=mule1
export MULE_APP
mule stop

sleep 5

# Only the second app should be running
MULE_APP=mule1
export MULE_APP
echo "mule1 should be stopped"
mule status
MULE_APP=mule2
export MULE_APP
echo "mule2 should be running"
mule status

sleep 1

# Start the first app again
MULE_APP=mule1
export MULE_APP
mule start

sleep 5

# Make sure both apps are running
MULE_APP=mule1
export MULE_APP
echo "mule1 should be running"
mule status
MULE_APP=mule2
export MULE_APP
echo "mule2 should be running"
mule status

sleep 1

# Stop the second app
MULE_APP=mule2
export MULE_APP
mule stop

sleep 5

# Only the first app should be running
MULE_APP=mule1
export MULE_APP
echo "mule1 should be running"
mule status
MULE_APP=mule2
export MULE_APP
echo "mule2 should be stopped"
mule status

sleep 1

# Stop the first app
MULE_APP=mule1
export MULE_APP
mule stop

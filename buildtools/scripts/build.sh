#!/bin/sh

##############################################
# Full nightly build for Mule
##############################################

# Purge old Mule artifacts from repository
rm -rf $M2_REPO/org/mule/

# Check-out project if necessary, otherwise update.
if [ ! -d "mule" ]
then
    svn co http://svn.codehaus.org/mule/trunk/mule mule
    cd mule
else
    cd mule
    svn update
fi

# Clean everything
mvn -Ptests,distributions clean

# Build all modules
mvn -Dmaven.test.skip=true -Ptests compile test-compile install

# Run unit tests only
mvn -Dmaven.test.failure.ignore=true test

# Run unit and integration tests
# mvn -Dmaven.test.failure.ignore=true -Ptests test

# Generate HTML reports
# mvn -Dmaven.test.skip=true -Ptests site:site

# Generate javadocs
mvn -Dmaven.test.skip=true -Ptests -Daggregate=true javadoc:javadoc

# Generate source bundles
mvn -Dmaven.test.skip=true -Ptests source:jar

# Generate all distributions
mvn -Dmaven.test.skip=true -Pdistributions install

# Upload all modules, javadocs, and sources to the public repository
# mvn -Dmaven.test.skip=true -Ptests -DperformRelease=true deploy

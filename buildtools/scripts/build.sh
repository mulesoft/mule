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
mvn -Ptests clean

# Build all modules
mvn -Dmaven.test.skip.exec=true -Ptests install

# Run unit tests only
mvn -Dmaven.test.failure.ignore=true test

# Run unit and integration tests
# mvn -Dmaven.test.failure.ignore=true -Ptests test

# Generate HTML reports
# mvn -Dmaven.test.skip.exec=true -Ptests site:site

# Generate javadocs
mvn -Dmaven.test.skip.exec=true -Ptests -Daggregate=true javadoc:javadoc

# Generate all modules (-DperformRelease=true to generate source bundles,
# will be repackaged by the full distro script into a single source zip)
mvn -Dmaven.test.skip.exec=true -DperformRelease=true install

# Upload all modules, javadocs, and sources to the public repository
# mvn -Dmaven.test.skip.exec=true -Ptests -DperformRelease=true deploy

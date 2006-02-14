#!/bin/sh
#
# Script to add a library to the Mule dependencies repository
#
# USAGE
# 	add-dependency.sh filename
#
# This script will properly add a library (i.e., jar file) to the Mule dependencies repository.
# The user is prompted for the groupId, artifactId, and version.  With this information, the 
# expected Maven 2.x directory structure is generated, along with a minimal POM and 
# MD5 checksums.  
#

DEPENDENCIES="/home/projects/mule/dist/dependencies"
# Use the -l option to generate hexadecimal letters as lowercase (expected by Maven).
MD5="/home/projects/mule/bin/md5 -l"

if [ $# = 1 ]
then
	echo "Enter groupId:"
	read groupId
	echo "Enter artifactId:"
	read artifactId
	echo "Enter version:"
	read version

	path=$DEPENDENCIES/$groupId/$artifactId/$version
	file=$artifactId-$version

	echo "Creating directory $path"
	mkdir -p $path	
	echo "Setting directory privileges"
	chmod -R 775 $DEPENDENCIES/$groupId
	chown -R :mule $DEPENDENCIES/$groupId

	echo "Installing library as $artifactId-$version.jar"
	cp $1 $path/$file.jar
	
	echo "Generating basic POM"	
	echo "<project>" > $path/$file.pom
	echo "  <modelVersion>4.0.0</modelVersion>" >> $path/$file.pom
	echo "  <groupId>$groupId</groupId>" >> $path/$file.pom
	echo "  <artifactId>$artifactId</artifactId>" >> $path/$file.pom
	echo "  <version>$version</version>" >> $path/$file.pom
	echo "</project>" >> $path/$file.pom

	echo "Generating checksums"	
	$MD5 $path/$file.jar > $path/$file.jar.md5
	$MD5 $path/$file.pom > $path/$file.pom.md5

	echo "Setting file privileges"
	chmod 664 $path/*
	chown :mule $path/*
else
	echo "Usage: add-dependency.sh filename"
fi

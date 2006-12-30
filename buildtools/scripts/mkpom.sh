#!/bin/sh
#
# Script to create an m2 POM from a library and its related artifacts
#
# USAGE
#     mkpom.sh file.jar [file-sources.jar] [file-javadoc.jar]
#

# The target repository path
DEPENDENCIES="."

# Maven expects hexadecimal letters as lowercase so you might have to add -l here
MD5="md5sum"

if [ $# = 1 ]
then
    echo "Enter groupId (can be multi-part, such as javax.mail):"
    read groupId
    echo "Enter artifactId:"
    read artifactId
    echo "Enter version:"
    read version
    echo "Enter classifier:"
    read classifier


    # Convert the group's package to a path (e.g. javax.mail --> javax/mail)
    groupPath=`echo $groupId | sed s*[.]*/*g`

    # Create file name
    path=$DEPENDENCIES/$groupPath/$artifactId/$version
    if [[ "$classifier" > "" ]]; then
        file=$artifactId-$version-$classifier
    else
        file=$artifactId-$version
    fi

    echo "Creating directory $path"
    mkdir -p $path
    echo "Setting directory privileges"
    chmod -fR 775 $DEPENDENCIES

    echo "Installing library as $file.jar"
    cp $1 $path/$file.jar

    echo "Generating basic POM"
    echo "<project>" > $path/$file.pom
    echo "  <modelVersion>4.0.0</modelVersion>" >> $path/$file.pom
    echo "  <groupId>$groupId</groupId>" >> $path/$file.pom
    echo "  <artifactId>$artifactId</artifactId>" >> $path/$file.pom
    echo "  <version>$version</version>" >> $path/$file.pom
    if [[ "$classifier" > "" ]] ; then
        echo "  <classifier>$classifier</classifier>" >> $path/$file.pom
    fi
    echo "</project>" >> $path/$file.pom

    echo "Generating checksums"
    $MD5 $path/$file.jar > $path/$file.jar.md5
    $MD5 $path/$file.pom > $path/$file.pom.md5

    # optional sources
    sources=`basename $1 .jar`-sources.jar
    if [ -f $sources ]; then
        echo "Copying sources to $file-sources.jar"
        cp $sources $path/$file-sources.jar
        $MD5 $path/$file-sources.jar > $path/$file-sources.jar.md5
    fi

    # optional javadoc
    javadoc=`basename $1 .jar`-javadoc.jar
    if [ -f $javadoc ]; then
        echo "Copying javadoc to $file-javadoc.jar"
        cp $javadoc $path/$file-javadoc.jar
        $MD5 $path/$file-javadoc.jar > $path/$file-javadoc.jar.md5
    fi

    echo "Setting file privileges"
    chmod 664 $path/*
else
    echo "Usage: `basename $0` <file>.jar [<file>-sources.jar] [<file>-javadoc.jar]"
fi

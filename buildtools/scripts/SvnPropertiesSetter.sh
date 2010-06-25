#!/bin/bash 

# This scripts sets the svn properties according to this wiki:
# http://www.mulesoft.org/documentation/display/MULECDEV/Subversion
# It must be executed from inside the svn working copy. That working copy should be clean (mvn clean)

find -name "*.java" -o -name "*.xml" -o -name "*.properties" | xargs svn ps svn:eol-style native ;


for i in `find -name "*.java" -o -name "*.xml" -o -name "*.properties"`; do
  keyword=`svn pg svn:keywords $i --strict`; 
  if [ "$keyword" != "Id Author Date Revision" ] && [  "$keyword" != "Author Date Id Revision" ] ; then
    echo $i; 
  fi;
done | xargs svn ps svn:keywords "Author Date Id Revision" ;


#! /bin/sh

#TODO DZ: only works in my environment!
export CLASSPATH=/home/dzapata/downloads/groovy-xmlrpc-0.4.jar:$CLASSPATH
groovy SvnJiraParser.groovy $*

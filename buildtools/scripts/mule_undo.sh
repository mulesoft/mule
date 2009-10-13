#!/bin/bash

# used in the same way as mule_root.sh, but reverts to backup file
# (apply to the .xml file, not the backup)

if [ -z "$1" ]
then
    echo "supply file name as first parameter"
    exit 1
fi
file="$1"

if [ ! -e "$file" ]
then
    echo "no file $file"
    exit 1
fi

line=`egrep '<mule xmlns="http://www.mulesoft.org/schema/mule/core"' "$file"`
if [ -z "$line" ]
then
    echo "file does not start with mule element"
    exit 0
fi

# revert to backup
if [ -e "${file}~" ]
then
    echo "${file}~ -> ${file}"
    mv "${file}~" "${file}"
fi

#!/bin/sh

# Usage: tabstospaces.sh directory"

if [ $# -lt 1 ]
then
    echo "Usage: $0 directory"
    exit 1
fi

ignore_dirs="\.svn\|CVS"
source_files="\.java$\|\.xml$\|\.xsd$\|\.xsl$\|\.dtd$\|\.txt$\|\.groovy$\|\.vm$\|\.properties$\|\.jsp$\|\.html$\|\.htm$\|\.sh$\|*README*"

for file in `find $1 | grep -v -e $ignore_dirs | grep -e $source_files`
do
    if [ -f $file ]
    then
        echo "Editing file $file"
        sed -e 's/\t/    /g' $file > $file.tmp
        mv -f $file.tmp $file
    fi
done

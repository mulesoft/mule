#!/bin/sh

# This script is useful for making a release with m2.  It simply replaces
# the release version in all POMs in the project.
#
# Usage: update_poms.sh directory old_version new_version"

if [ $# -lt 3 ]
then
    echo "Usage: $0 directory old_version new_version"
    exit 1
fi

echo "Replacing $2 with $3 for all pom.xml files in directory $1..."

for pom in `find $1 -name pom.xml`
do
    echo "Editing file $pom"
    sed -e 's/'"$2"'/'"$3"'/g' $pom > $pom.tmp
    mv -f $pom.tmp $pom
done

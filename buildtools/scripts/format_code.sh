#!/bin/bash
#
#Formats all the code under the supplied directory.
#   First parameter can be the root directory to start running the command from.
#   If no parameter is provided then the script calling directory will be used.
#
# USAGE
#   - format_code.sh [extensions/]
#

if [[ -z "$1" ]]; then
  rootDir=$(pwd);
else
  rootDir=$(pwd);
  rootDir="$rootDir/$1";
fi
dirs=$(find "$rootDir" -maxdepth 5 -type d);
cd "$(pwd)" || exit;
for dir in $dirs;
do
  hasPom=$(ls "$dir" | grep pom.xml);
  if [[ ! -z "$hasPom" ]]; then
    cd "$dir" || exit;
    (mvn formatter:format || true);
  fi
done


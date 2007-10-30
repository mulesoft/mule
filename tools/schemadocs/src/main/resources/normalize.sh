#!/bin/bash

# this is now replaced by org.mule.buildtools.schemadoc.Main

# this generates a single composite xsd
# the resulting file is not valid as a schema, but allows easier cross-referencing of elements
# (the underlying problem is that while xsl handles namespaces, when we select with xpath on,
# say, a element name, that's just a string; even worse, xsd doesn't seem to support namespaces
# on types explicitly)

NORMALIZED="normalized.xsd"
BACKUP=".backup"

if [ -e "$NORMALIZED" ]
then
  if [ -e "$NORMALIZED$BACKUP" ]
  then
    rm "$NORMALIZED$BACKUP"
  fi
  mv "$NORMALIZED" "$NORMALIZED$BACKUP"
fi

cat schemadoc-prefix.txt > "$NORMALIZED"

for file in `find ../../.. \( -name "normalized.xsd" -prune \) -o \( -name "buildtools" -prune \) -o \( -name "*classes" -prune \) -o \( -name "test" -prune \) -o -name "*.xsd" -print `
do
  tag=`echo "$file" | sed -e "s/.*mule-\?\(.*\)\.xsd/\1/"`
  if [ -z "$tag" ]
  then
    tag="mule"
  fi
  echo "$tag : $file"
  saxon "$file" rename-tag.xsl tag="$tag" >> "$NORMALIZED"
done

cat schemadoc-postfix.txt >> "$NORMALIZED"

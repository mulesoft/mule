#!/bin/bash

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

cat > "$NORMALIZED" << EOS
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<xsd:schema xmlns="http://www.mulesource.org/schema/mule/core/2.0"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xmlns:xsd="http://www.w3.org/2001/XMLSchema"
            xmlns:spring="http://www.springframework.org/schema/beans"
            attributeFormDefault="unqualified"
            elementFormDefault="qualified">

    <xsd:import namespace="http://www.w3.org/XML/1998/namespace"/>
    <xsd:import namespace="http://www.springframework.org/schema/beans"
                schemaLocation="http://www.springframework.org/schema/beans/spring-beans-2.0.xsd"/>
EOS

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

cat >> "$NORMALIZED" << EOS
</xsd:schema>
EOS

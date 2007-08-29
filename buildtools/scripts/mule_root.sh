#!/bin/bash

# this was used to convert spring-based configs to use the new mule root element
# typical use was
#  find some/directory -name "*.xml" -exec path/mule_root.sh \{} \;
# see mule_undo.sh for automated reverting to backup

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

line=`egrep '<beans xmlns="http://www.springframework.org/schema/beans"' "$file"`
if [ -z "$line" ]
then
    echo "file does not start with beans element"
    exit 0
fi

# make a backup
if [ -e "${file}~" ]
then
    rm "${file}~"
fi
cp "${file}" "${file}~"

# change root element
sed -i -e 's/<beans xmlns="http:\/\/www.springframework.org\/schema\/beans"/<mule xmlns="http:\/\/www.mulesource.org\/schema\/mule\/core\/2.0"/' "$file"
sed -i -e 's/<\/beans>/<\/mule>/' "$file"
# change mule ns for spring
sed -i -e 's/xmlns:mule="http:\/\/www.mulesource.org\/schema\/mule\/core\/2.0"/xmlns:spring="http:\/\/www.springframework.org\/schema\/beans"/' "$file"
# remove mule: prefixes
sed -i -e 's/<mule:/</' "$file"
sed -i -e 's/<\/mule:/<\//' "$file"
# make spring namespace explicit
sed -i -e 's/<bean /<spring:bean /' "$file"
sed -i -e 's/<\/bean>/<\/spring:bean>/' "$file"
sed -i -e 's/<entry /<spring:entry /' "$file"
sed -i -e 's/<\/entry>/<\/spring:entry>/' "$file"
sed -i -e 's/<property /<spring:property /' "$file"
sed -i -e 's/<\/property>/<\/spring:property>/' "$file"
sed -i -e 's/<list>/<spring:list>/' "$file"
sed -i -e 's/<\/list>/<\/spring:list>/' "$file"
sed -i -e 's/<map>/<spring:map>/' "$file"
sed -i -e 's/<\/map>/<\/spring:map>/' "$file"
sed -i -e 's/<value>/<spring:value>/' "$file"
sed -i -e 's/<\/value>/<\/spring:value>/' "$file"
sed -i -e 's/<constructor-arg>/<spring:constructor-arg>/' "$file"
sed -i -e 's/<\/constructor-arg>/<\/spring:constructor-arg>/' "$file"

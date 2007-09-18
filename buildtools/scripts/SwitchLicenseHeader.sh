#!/bin/bash

for file in `find . -name \*.java -print`
do
    sed 's/MuleSource MPL/CPAL v1.0/g' < $file > $file.new
    mv $file.new $file
done

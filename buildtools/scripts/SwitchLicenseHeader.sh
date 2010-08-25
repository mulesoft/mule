#!/bin/bash

for file in `find . -name \*.html -print`
do
    sed 's/MuleSource/MuleSoft/g' < $file > $file.new
    mv $file.new $file
done

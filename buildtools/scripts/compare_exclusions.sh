#!/bin/bash

if [ $# -ne 2 ]
then
    echo "Usage: $0 directory1 directory2"
    exit 1
fi

dir1="$1"
dir2="$2"

function name() {
  dir="$1"
  full=`readlink -f "$dir"`
  name=`basename "$full"`
}

function process() {
  dir="$1"
  name="$2"
  echo "reading $name"
  tmp=`mktemp "${name}.XXX"`
  find "$dir" -name mule-test-exclusions.txt -exec egrep "^[a-z]" \{} \; | sort > "$tmp"
  
}

function display() {
  file1="$1"
  file2="$2"
  name="$3"
  echo
  echo
  echo -n "tests excluded only in $name: "
  comm -2 -3 "$file1" "$file2" | wc -l
  echo
  comm -2 -3 "$file1" "$file2"
}

name "$dir1"
namedir1="$name"
name "$dir2"
namedir2="$name"

process "$dir1" "$namedir1"
tmpdir1="$tmp"
process "$dir2" "$namedir2"
tmpdir2="$tmp"

display "$tmpdir1" "$tmpdir2" "$namedir1"
display "$tmpdir2" "$tmpdir1" "$namedir2"

echo
echo
echo -n "tests excluded in both: "
comm -1 -2 "$file1" "$file2" | wc -l
echo
comm -1 -2 "$tmpdir1" "$tmpdir2"

#rm "$tmpdir1" "$tmpdir2"

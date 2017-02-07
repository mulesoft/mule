#!/bin/sh

updatePropertiesVersion() {
  VERSION_TO_PROPERTY="$1"
  POM_PROPERTY_PATH="$2"

  # PROPERTIES argument should be passed as a literal "arrayName[@]" without $ because here using the ! it is double expanded
  # to obtiain the values and declare again the array.
  PROPERTIES=("${!3}")

  echo "Updating deps in pom: $POM_PROPERTY_PATH"

  for PROPERTY_NAME in "${PROPERTIES[@]}"
  do

      perl -0777 -i -pe "s/(<properties>.*<$PROPERTY_NAME)(.*)(\/$PROPERTY_NAME>.*<\/properties>)/\${1}>$VERSION_TO_PROPERTY<\${3}/s" "$POM_PROPERTY_PATH"
      echo "- Updating property $PROPERTY_NAME version to $VERSION_TO_PROPERTY"

  done
}

VERSION_TO=$1

# Properties with releaseVersion in the root pom.xml
propertiesDeps=("mule.module.maven.plugin.version"
                "mule.extensions.maven.plugin.version"
                "mule.app.plugins.maven.plugin.version")

updatePropertiesVersion "$VERSION_TO" pom.xml propertiesDeps[@]




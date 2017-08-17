#!/bin/sh

set -o nounset

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

updateParentVersion() {
  VERSION_TO="$1"
  POM_PROPERTY_PATH="$2"

  echo "Updating version to $VERSION_TO..."
  perl -0777 -i -pe "s/(<parent>.*<version)(.*)(\/version>.*<\/parent>)/\${1}>$VERSION_TO<\${3}/s" "$POM_PROPERTY_PATH"
}

VERSION_TO_DEPS=$1
VERSION_CONNECTORS_MODULES=$2
VERSION_TO_MULE=$3
VERSION_TO_DEPS_AUX=$4
VERSION_CONNECTORS_MODULES_AUX=$5


# Properties with Deps Version (1.0.x) in the root pom.xml
propertiesDeps=("muleEmbeddedApiVersion")

updatePropertiesVersion "$VERSION_TO_DEPS" distributions/pom.xml propertiesDeps[@]


# Properties with Deps Version (1.0.x) in the root pom.xml
propertiesDeps=("mule.module.maven.plugin.version"
                "mule.extensions.maven.plugin.version"
                "mule.app.plugins.maven.plugin.version"
                "muleMavenClientTest")

updatePropertiesVersion "$VERSION_TO_DEPS" pom.xml propertiesDeps[@]


# Properties with 0.8.x in the root pom.xml
propertiesDeps=("muleHttpServiceTestVersion"
                "muleSchedulerServiceTestVersion")

# Using Snapshot as a workaround because the extensions have the mule artifacts as
# deps. So there is a circular dependency.
updatePropertiesVersion "$VERSION_TO_DEPS_AUX" pom.xml propertiesDeps[@]


# Properties with 0.8.x in the root pom.xml
propertiesDeps=("muleHttpConnectorTestVersion"
                "muleSocketsConnectorTestVersion"
                "muleFileConnectorTestVersion"
                "muleJmsConnectorTestVersion"
                "muleFileCommonsTestVersion"
                "muleSpringModuleTestVersion")

# Using Snapshot as a workaround because the extensions have the mule artifacts as
# deps. So there is a circular dependency.
updatePropertiesVersion "$VERSION_CONNECTORS_MODULES_AUX" pom.xml propertiesDeps[@]


propertiesDeps=("muleMetadataExtensionTestVersion"
                "muleVeganExtensionTestVersion"
                "muleHeisenbergExtensionTestVersion"
                "muleSubtypesExtensionTestVersion"
                "muleMarvelExtensionTestVersion"
                "mulePetstoreExtensionTestVersion")

updatePropertiesVersion "$VERSION_TO_MULE" pom.xml propertiesDeps[@]

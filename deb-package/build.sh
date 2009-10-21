#! /bin/sh

MULE_VERSION=2.2.3
MULE_TARBALL=../standalone/target/mule-standalone-*.tar.gz

echo "Building Debian package for Mule version ${MULE_VERSION} based on the following tarball:"
ls ${MULE_TARBALL}
echo

rm -rf target
mkdir target

# Extract the tarball and give it a proper name
tar xfz ${MULE_TARBALL} --directory target
mv target/mule-* target/mule-${MULE_VERSION}
# Overwrite a few Mule files from the standard distribution
cp mule/mule target/mule-${MULE_VERSION}/bin/
cp mule/wrapper.conf target/mule-${MULE_VERSION}/conf/
# Add the Debian control files
cp -r debian target/mule-${MULE_VERSION}/

# Build the package
cd target/mule-${MULE_VERSION}/
debuild -us -uc -b


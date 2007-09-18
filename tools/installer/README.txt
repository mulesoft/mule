Building the Mule GUI Installer Using IzPack Version 3.10

1.) Unpack the full distribution from distributions/server/full in the
    mule/tools/installer/distribution folder. This is where the install.xml
    is set to look for the Mule Distribution folders.
    The distribution folder must contain the contents of the distribution directly,
    i.e. the bin, sbin, examples folders etc.

2.) Download the latest IzPack distribution from: http://izpack.org/downloads.
    Make sure you also download the sources as we're going to build the installer
    with Mule extensions later.

3.) Set the IZPACK_HOME environment variable pointing to the location where your
    IzPack distribution is installed

4.) Copy custom/src/izpack to %IZPACK_HOME%/src/lib/com

5.) Add the following configuration to the IzPack build file
    (%IZPACK_HOME%/src/build.xml under build.listeners) or substitute the 
    build file with the one provided in 
    mule\tools\installer\Modified IzPack Build File :
        <build-installer-listener name="MuleInstallerListener">
            <include name="com/izpack/mule/installer/custom/listener/MuleInstallerListener.java"/>
        </build-installer-listener>

6.) Use the ant tool to rebuild IzPack

7.) Verify that %IZPACK_HOME%/bin/customActions has a MuleInstallerListener.jar and
    that the jar contains the custom class you copied over in step 4.

8.) Update the Mule version number in config/install.xml

8.) Build the Mule installer from the mule/tools/installer/config folder using
    the following command:
        %IZPACK_HOME%/bin/compile install.xml -b . -o install.jar -k standard
    This will create an install.jar

9.) Test the installer jar by using
        java -jar installer.jar

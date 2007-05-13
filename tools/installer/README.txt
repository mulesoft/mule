Building the Mule GUI Installer Using IzPack Version 3.10

1.) Put the individual Mule Distribution Files in the mule/tools/installer/distribution folder. This is where the install.xml is set to look for the Mule Distribution folders
2.) Download the latest IzPack distribution from: http://izpack.org/downloads
3.) Set the IzPack_Home as an environment variable pointing to the location where your IzPack distribution is installed
4.) Add the MuleInstallerListener.jar and the provided MuleInstallerListener folder in the following location:
	%IZPACK_HOME%/bin/customActions
5.) Add the following configuration to the IzPack build file (under build.listeners) or substitute the build file with the one provided in mule\tools\installer\Modified IzPack Build File
	
	<build-installer-listener name="MuleInstallerListener">
	      <include name="com/izpack/mule/installer/custom/listener/MuleInstallerListener.java"/>
	</build-installer-listener>	

6.) Use the ant tool to rebuild IzPack
7.) Build the Mule installer from the mule/tools/installer/config folder using the following command:
	%IZPACK_HOME%/bin/compile install.xml -b . -o install.jar -k standard
    This will create an install.jar
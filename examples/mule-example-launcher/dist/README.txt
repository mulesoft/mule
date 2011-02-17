+-----------------------+
| Mule Example Launcher |
+-----------------------+
Launches Mule examples from within a simple web application, displaying an introduction
to the example, the mule configuration file and when possible, provides interaction with
the example.

** IMPORTANT **
This example application is meant to be used together with other examples that ship with
Mule ESB. The correct way to use it is by launching mule with the following command:

$MULE_HOME/bin/mule_examples

This command will deploy the examples required by the Example Launcher. 

For more information visit:
http://www.mulesoft.org/documentation/display/MULE3INTRO/Examples

+---------------------+
| Running the example |
+---------------------+
Simply build the example and copy the packaged application to Mule's application
folder ($MULE_HOME/apps).

+----------------------+
| Building the example |
+----------------------+
First, make sure you have set the MULE_HOME environment variable as recommended
in Mule's README.txt

Depending on the build tool you are using (Ant or Maven), you can build the example
by simply running "ant" or "mvn".  This will compile the example classes and produce
an application archive file.

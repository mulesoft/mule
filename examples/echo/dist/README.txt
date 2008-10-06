+--------------+
| Echo Example |
+--------------+
This example demonstrates how to expose a Mule component over multiple 
transports, in this case as an Axis web sevice and via System.in (request) and 
System.out (response).

For more information, refer to http://mule.mulesource.org/display/MULE2INTRO/Examples

+---------------------+
| Running the example |
+---------------------+
Simply use the shell script (Unix/Linux) or batch file (Windows) provided in 
this directory to run the example.

Alternatively, if you have added Mule to your executable path as recommended in
Mule's README.txt, you can run the example from the command line as follows:

    Linux / Unix
    ------------
    mule -config file:conf/echo-config.xml
     or
    export MULE_LIB=./conf
    mule -config echo-config.xml

    Windows
    -------
    mule.bat -config file:conf/echo-config.xml
     or
    SET MULE_LIB=.\conf
    mule.bat -config echo-config.xml

+----------------------+
| Building the example |
+----------------------+
The only custom classes in here are used by CXF. These must be built using 
Java 1.5 because they use annotations.

First, make sure you have set the MULE_HOME environment variable as recommended
in Mule's README.txt

Depending on the build tool you are using (Ant or Maven), you can build the example
by simply running "ant" or "mvn".  This will compile the example classes, produce a
jar file, and copy everything to $MULE_HOME/lib/user, which is where your custom classes
and configuration files should go.

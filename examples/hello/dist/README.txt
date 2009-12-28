+---------------------+
| Hello World Example |
+---------------------+
This example uses two components to create a hello world message. After you start the 
example, go to your browser to run it:
    http://localhost:8888/?name=Ross
You can change 'Ross' to any string you want.

For more information, refer to http://mule.mulesource.org/display/MULE2INTRO/Examples

+---------------------+
| Running the example |
+---------------------+
Simply use the shell script (Unix/Linux) or batch file (Windows) provided in 
this directory to run the example.

To invoke the hello component over http, hit the following URL

    http://localhost:8888/?name=Ross

+----------------------+
| Building the example |
+----------------------+
First, make sure you have set the MULE_HOME environment variable as recommended 
in Mule's README.txt

Depending on the build tool you are using (Ant or Maven), you can build the 
example by simply running "ant" or "mvn".  This will compile the example 
classes, produce a jar file, and copy everything to $MULE_HOME/lib/user, which 
is where your custom classes should go.

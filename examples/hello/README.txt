+---------------------+
| Hello World Example |
+---------------------+
This example uses two components to create a hello world message. When the example starts
it prompts the user at the console to type in their name, the user's name is then passed
to the first component which adds something to the string before passes on to the second
component that also adds some text before outputting the results back to the console.

For more information, refer to http://www.muledocs.org/Examples

+---------------------+
| Running the example |
+---------------------+
Simply use the shell script (Unix/Linux) or batch file (Windows) provided in this directory to run
the example.

Alternatively, if you have added Mule to your executable path as recommended in INSTALL.txt, you
can run the example from the command line as follows:

    Linux / Unix
    ------------
    mule -config ./conf/hello-config.xml
    mule -config ./conf/hello-http-config.xml
    mule -config ./conf/hello-spring-config.xml -builder spring

    Windows
    -------
    mule.bat -config .\conf\hello-config.xml
    mule.bat -config .\conf\hello-http-config.xml
    mule.bat -config .\conf\hello-spring-config.xml -builder spring

To invoke the hello component over http, hit the following URL

    http://localhost:8888/?name=Ross

+----------------------+
| Building the example |
+----------------------+
First, make sure you have set the MULE_HOME environment variable as recommended in INSTALL.txt

Depending on the build tool you are using (Ant or Maven), you can build the example by simply
running "ant" or "mvn".  This will compile the example classes, produce a jar file, and copy
everything to $MULE_HOME/lib/user, which is where your custom classes and configuration files
should go.

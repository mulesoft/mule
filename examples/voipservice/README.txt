+----------------------+
| VoIP Service Example |
+----------------------+
This example is described in the feature Java.net article
"Service Provisioning Through ESB".

For more information, refer to http://mule.mulesource.org/Examples

+---------------------+
| Running the example |
+---------------------+
Simply use the shell script (Unix/Linux) or batch file (Windows) provided in this directory to run 
the example.

Alternatively, if you have added Mule to your executable path as recommended in INSTALL.txt, you 
can run the example from the command line as follows:

    mule -main org.mule.samples.voipservice.client.VoipConsumer

+----------------------+
| Building the example |
+----------------------+
First, make sure you have set the MULE_HOME environment variable as recommended in INSTALL.txt

Depending on the build tool you are using (Ant or Maven), you can build the example by simply 
running "ant" or "mvn".  This will compile the example classes, produce a jar file, and copy 
everything to $MULE_HOME/lib/user, which is where your custom classes and configuration files 
should go.  

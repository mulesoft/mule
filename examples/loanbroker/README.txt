+---------------------+
| Loan Broker Example |
+---------------------+
The application design is based on the Loan Broker example presented in the Enterprise
Integration Patterns book.

This example demonstrates the Loan Broker without using a shared message bus, but rather
an Enterprise Service Network approach (no shared message type and components interacting
directly).  The example demonstrates Synchronous and Asynchronous message processing
styles.

+---------------------+
| Running the example |
+---------------------+
Simply use the shell script (Unix/Linux) or batch file (Windows) provided in this directory to run 
the example.

Alternatively, if you have added Mule to your executable path as recommended in INSTALL.txt, you 
can run the example from the command line as follows:

    mule -main org.mule.samples.loanbroker.LoanConsumer

+----------------------+
| Building the example |
+----------------------+
First, make sure you have set the MULE_HOME environment variable as recommended in INSTALL.txt

Depending on the build tool you are using (Ant or Maven), you can build the example by simply 
running "ant" or "mvn".  This will compile the example classes, produce a jar file, and copy 
everything to $MULE_HOME/lib/user, which is where your custom classes and configuration files 
should go.  

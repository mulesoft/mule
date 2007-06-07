+-----------------+
| Web App Example |
+-----------------+
The Web App provides a user interface to some of the Mule examples such
as the LoanBroker, Hello World and Echo examples. It also provides examples of
accessing Mule using REST style service calls and is itself an example of how
to embed Mule in a webapp.

For more information, refer to http://www.muledocs.org/Mule+Examples+Webapp

+---------------------+
| Running the example |
+---------------------+
First, make sure you have set the MULE_HOME environment variable as recommended in INSTALL.txt

Depending on the build tool you are using (Ant or Maven), you can build the example by simply 
running "ant" or "mvn".  This will generate a WAR file in the "target" directory which can be 
deployed to any standard JEE web server.

By default, the "mvn" build will automatically start up the WAR in Jetty!  Just surf to the 
following URL in your browser:

http://localhost:8090/mule-examples


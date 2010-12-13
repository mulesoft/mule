+--------------+
| Echo Example |
+--------------+
This example demonstrates how to expose a Mule component over multiple
transports, in this case as a CXF web sevice.

For more information, refer to 
http://www.mulesoft.org/documentation/display/MULE3INTRO/Echo+Example

+---------------------+
| Running the example |
+---------------------+
Simply copy the pre-built application archive (mule-example-echo.zip) to the
application folder ($MULE_HOME/apps) and start Mule. To access the web service
invoke

    http://localhost:65082/services/EchoUMO/echo/text/hello

from your browser.

+----------------------+
| Building the example |
+----------------------+
The only custom classes in here are used by CXF. These must be built using
Java 1.5 because they use annotations.

Depending on the build tool you are using (Ant or Maven), you can  build the
example by simply running "ant" or "mvn".  This will compile the example classes
and produce a zip file that is automatically copied into the application folder
($MULE_HOME/apps).

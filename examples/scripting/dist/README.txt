+-------------------+
| Scripting Example |
+-------------------+
This example demonstrates Mule's JSR-223 Scripting support.

For more information, refer to http://www.mulesoft.org/documentation/display/MULE3INTRO/Scripting+Example

+---------------------+
| Running the example |
+---------------------+
Simply copy the pre-built application archive (mule-example-scripting.zip) to the
application folder ($MULE_HOME/apps) and start Mule. To access the web service 
invoke one of the following:

    http://localhost:47493/change-machine?amount=2.42&currency=USD
    http://localhost:47493/change-machine?amount=2.42&currency=GBP

from your browser.

+----------------------+
| Building the example |
+----------------------+
First, make sure you have set the MULE_HOME environment variable as recommended
in Mule's README.txt

Depending on the build tool you are using (Ant or Maven), you can  build the 
example by simply running "ant" or "mvn".  This will compile the example classes
and produce a zip file that can be copied into the application folder 
($MULE_HOME/apps).

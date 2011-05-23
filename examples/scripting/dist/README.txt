+-------------------+
| Scripting Example |
+-------------------+
This example demonstrates Mule's JSR-223 Scripting support.

For more information, refer to 
http://www.mulesoft.org/documentation/display/MULE3EXAMPLES/Scripting+Example

+---------------------+
| Running the example |
+---------------------+
Simply copy the pre-built application archive (mule-example-scripting.zip) to 
the application folder ($MULE_HOME/apps) and start Mule. To access the web 
service invoke one of the following URIs from your browser:

    http://localhost:47493/change-machine?amount=2.42&currency=USD
    http://localhost:47493/change-machine?amount=2.42&currency=GBP

+----------------------+
| Building the example |
+----------------------+
Depending on the build tool you are using (Ant or Maven), you can  build the 
example by simply running "ant" or "mvn".  This will compile the example 
classes and produce a zip file that can be copied into the application folder 
($MULE_HOME/apps).

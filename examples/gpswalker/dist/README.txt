+--------------------+
| GPS Walker Example |
+--------------------+
This example demonstrates how to use AJAX to communicate from a Mule Service to
the browser. This example uses other new features in Mule 3 including automatic
JSON bindings, the @Schedule annotation and Flow.

For more information, refer to 
http://www.mulesoft.org/documentation/display/MULE3EXAMPLES/GPS+Walker+Example

+---------------------+
| Running the example |
+---------------------+
Simply copy the pre-built application archive (mule-example-gpswalker.zip) to 
the application folder ($MULE_HOME/apps) and start Mule. To access the web 
service invoke the following URI from your browser:

    http://localhost:8081/services/gps/index.html

+----------------------+
| Building the example |
+----------------------+
You can build the example by simply running "mvn".  This will compile the 
example classes and produce a zip file that will be copied into the application 
folder ($MULE_HOME/apps).

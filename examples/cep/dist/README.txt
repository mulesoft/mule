+----------------------------+
|        CEP Example         |
+----------------------------+
This example demonstrates how to add CEP (Complex Event Processing) 
capabilities to Mule using Drools.

For more information, refer to 
http://www.mulesoft.org/documentation/display/MULE3EXAMPLES/CEP+Example

+---------------------+
| Running the example |
+---------------------+
Copy the pre-built application archive (mule-example-cep.zip) to the 
application folder ($MULE_HOME/apps) and start Mule.  Once Mule is running 
open the following URL in your browser:

    http://localhost:8087/services/cepExample

You should see a periodic stock tick and less frequent stock alerts based on 
the defined rules.

+----------------------+
| Building the example |
+----------------------+
Run "mvn" from the example source folder.  This will compile the example
classes, produce an application zip file and copy it to $MULE_HOME/apps.

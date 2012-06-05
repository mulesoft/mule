+------------------------------------------+
|       Flight Reservation Example         |
+------------------------------------------+
This example simulates a flight reservation system using some of the new features added in Mule 3.3:

  - New transformers: properties, variables and set-payload
  - Foreach
  - Choice exception strategy
  - Expression component
  - Mule Expressions

For more information, refer to 
http://www.mulesoft.org/documentation/display/MULE3EXAMPLES/Flight+Reservation+Example

+---------------------+
| Running the example |
+---------------------+
Copy the pre-built application archive (mule-example-flight-reservation.zip) to the
application folder ($MULE_HOME/apps) and start Mule. Once Mule is running open the following URL in your browser:

    http://localhost:9092/reservation/index.html


+----------------------+
| Building the example |
+----------------------+
Run "mvn" from the example source folder.  This will compile the example
classes, produce an application zip file and copy it to $MULE_HOME/apps.

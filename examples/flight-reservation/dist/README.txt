+------------------------------------------+
|       Flight Reservation Example         |
+------------------------------------------+
This example simulates a flight reservation system using some of the new features added in Mule 3.3:

  - New property and variable transformers
  - Foreach
  - Choice exception strategy
  - Rollback exception strategy

A reservation service is provided through the HTTP protocol using JSON messages.

Input: selected flights
Output: Seat and price for each flight
        Reservation total price
        List of errors
        Original request in case of error

Note that:
  - Only flight numbers ended in 2 will have a seat assignment
  - Flights that end with 3 will show a no availability error message
  - Invalid request will show the message of the exception generated in mule

For more information, refer to 
http://www.mulesoft.org/documentation/display/MULE3EXAMPLES/Flight+Reservation+Example

+---------------------+
| Running the example |
+---------------------+
Copy the pre-built application archive (mule-example-flight-reservation.zip) to the
application folder ($MULE_HOME/apps) and start Mule. Once Mule is running send a JSON request to the
following URL:

    http://localhost:9092/reservation

Example of valid JSON request:

  - Valid request: {"flights": [{"flightNumber": 15},{"flightNumber": 22},{"flightNumber": 30}]}
  - No availability request: {"flights": [{"flightNumber": 15},{"flightNumber": 33}]}
  - Invalid request: {'not valid json'}


+----------------------+
| Building the example |
+----------------------+
Run "mvn" from the example source folder.  This will compile the example
classes, produce an application zip file and copy it to $MULE_HOME/apps.

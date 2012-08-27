+----------------------------+
| Loan Broker Simple Example |
+----------------------------+
This example demonstrates flows, the simple-service configuration pattern, 
writing transformers and filters in Groovy, and how to use CXF clients and 
services.

For more information, refer to 
http://www.mulesoft.org/documentation/display/MULE3EXAMPLES/Loan+Broker+Example

+---------------------+
| Running the example |
+---------------------+
Copy the pre-built application archive (mule-example-loanbroker-simple.zip) to 
the application folder ($MULE_HOME/apps) and start Mule. When Mule is running 
go to your browser to run it:

    http://localhost:11081?name=Muley&amount=20000&term=48&ssn=1234

You should get a response with the name of the bank providing the best deal and 
its interest rate.

Now, modify the loan amount requested, and see different banks responding.
Try different amounts (100, 10000, 20000). Also try invalid numbers to see the
error handling in action.

+----------------------+
| Building the example |
+----------------------+
Run "mvn" from the example source folder.  This will compile the example
classes, produce an application zip file and copy it to $MULE_HOME/apps.

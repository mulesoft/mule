+-------------------------+
| Loan Broker BPM Example |
+-------------------------+
The BPM example uses a business process engine to orchestrate a loan broker 
application.

For more information, refer to 
http://www.mulesoft.org/documentation/display/MULE3EXAMPLES/Loan+Broker+BPM+Example

+---------------------+
| Running the example |
+---------------------+
Copy the pre-built application archive (mule-example-loanbroker-bpm.zip) to the 
application folder ($MULE_HOME/apps) and start Mule. When Mule is running go to 
your browser to send a loan request (there are some default request values 
built-in):

    http://localhost:12081

In the Mule log file, you should see the process execute and a response message 
such as the following from one of the banks:

   "Returning Rate is: ABigBank, rate: 6.379575743481158"

Now, modify the loan amount requested, and see different banks responding based
on the amount:

    http://localhost:12081/?amount=100

Try different amounts (100, 10000, 20000). Also try invalid numbers to see the 
error handling in action.

+----------------------+
| Building the example |
+----------------------+
Run "mvn" from the example source folder.  This will compile the example 
classes, produce an application zip file and copy it to $MULE_HOME/apps.

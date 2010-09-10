+---------------------+
| Hello World Example |
+---------------------+
This example uses two components to create a hello world message. Simply copy
the pre-built application archive (mule-example-echo-app.zip) to the application
folder ($MULE_HOME/apps) and start Mule. When Mule is running go to your browser
to run it:

    http://localhost:11081

You should get a response with the name of the bank providing the best deal and its
interest rate. There are some default request values built-in for this demo.

Now, modify the loan amount requested, and see different banks responding:

    http://localhost:11081/?amount=20000

Try different amounts. Also try invalid numbers to see the error handling in action.

+----------------------+
| Building the example |
+----------------------+
First, make sure you have set the MULE_HOME environment variable as recommended 
in Mule's README.txt

Run "mvn" from the example source folder.  This will compile the example
classes, produce an application zip file and copy it to 
$MULE_HOME/apps.

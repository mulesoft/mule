+----------------------------+
| Loan Broker Simple Example |
+----------------------------+
Copy the pre-built application archive (mule-example-loanbroker-simple.zip) to the application
folder ($MULE_HOME/apps) and start Mule. When Mule is running go to your browser
to run it:

    http://localhost:11081

You will see something like this as your loan quote:
Bank #2, rate: 2.429746910752354

For more information, refer to http://www.mulesoft.org/documentation/display/MULE3INTRO/Examples

+----------------------+
| Building the example |
+----------------------+
First, make sure you have set the MULE_HOME environment variable as recommended 
in Mule's README.txt

You can build the example by simply running "mvn".  This will compile the example 
classes, produce an application zip file and copy it to $MULE_HOME/apps.

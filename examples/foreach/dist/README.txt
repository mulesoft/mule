+--------------------------------+
|        Foreach Example         |
+--------------------------------+
This example demonstrates how to use Foreach to add information to each message in a collection. It also takes
advantage of other features added in Mule 3.3 such as:

    - New set-payload and parse-template transformers
    - Catch Exception Strategy
    - Mule Expressions
    - HTTP Response Builder

For more information, refer to 
http://www.mulesoft.org/documentation/display/MULE3EXAMPLES/Foreach+Example

+---------------------+
| Running the example |
+---------------------+
Copy the pre-built application archive (mule-example-foreach.zip) to the
application folder ($MULE_HOME/apps) and start Mule.  Once Mule is running 
open the following URL in your browser:

    http://localhost:9091/populate

You should see a message indicating that the Database has been populated. Next enter the following URL:

    http://localhost:9091/process

You should see a message indicating the amount of records that have been processed.

+----------------------+
| Building the example |
+----------------------+
Run "mvn" from the example source folder.  This will compile the example
classes, produce an application zip file and copy it to $MULE_HOME/apps.

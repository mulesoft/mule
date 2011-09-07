+-----------------------+
| Mule Example Launcher |
+-----------------------+
This example provides a web interface to the Mule examples.  It displays an introduction
to the example and the mule configuration file.  You can also interact with certain examples 
through the web page.

For more information visit:
- http://www.mulesoft.org/documentation/display/MULE3EXAMPLES/Home
- http://www.mulesoft.org/documentation/display/MULE3EXAMPLES/Running+the+Examples+With+the+Example+Launcher

+---------------------+
| Running the example |
+---------------------+
This example application is meant to be used together with the other examples that ship with
Mule ESB. The correct way to use it is by launching mule with the following command:

$MULE_HOME/bin/mule_examples

This command will deploy the examples required by the Example Launcher.  Once the examples are
deployed, a browser window will pop up with the web interface for the examples launcher.  If
your browser does not open the examples launcher page, you can go to this url:

http://localhost:18082/examples/

+----------------------+
| Building the example |
+----------------------+
The Maven build files are provided, but it is not necessary to build the example in order to run it.

You can build the example by simply running "mvn".  This will compile the example classes and produce
an application archive file.

** IMPORTANT **
If you build the example this way, all the required examples that this example requires won't be deployed.
Check the information on running the example above.

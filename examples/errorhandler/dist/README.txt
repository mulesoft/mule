+-----------------------+
| Error Handler Example |
+-----------------------+
The Error handler example demonstrates using Spring as the container to provide
the objects managed by Mule and how to publish events to multiple outbound 
endpoints.  The example consists of two components: ExceptionManager and 
BusinessErrorManager.

For more information, refer to http://www.mulesoft.org/documentation/display/MULE3INTRO/Examples

+---------------------+
| Running the example |
+---------------------+
You will need two shell windows in order to run this application.

In one window, Simply copy the pre-built application archive 
(mule-example-errorhandler.zip) to the application folder ($MULE_HOME/apps) 
and start Mule.

If you want to receive emails when certain exceptions occur, update the $MULE_HOME/apps/mule-example-errorhandler/classes/email.properties file with your 
mail server credentials.  Then touch the $MULE_HOME/apps/mule-example-errorhandler/mule-config.xml
to reload the app.

In the second window, copy the files (one by one so you can see the results) 
from the $MULE_HOME/apps/mule-example-errorhandler/test-data/out directory to the 
$MULE_HOME/apps/mule-example-errorhandler/test-data/in directory.

+----------------------+
| Building the example |
+----------------------+
First, make sure you have set the MULE_HOME environment variable as recommended
in Mule's README.txt

Depending on the build tool you are using (Ant or Maven), you can build the 
example by simply running "ant" or "mvn".  This will compile the example 
classes and produce a zip file that will be copied into the application folder 
($MULE_HOME/apps).

(If you are unable to download the libraries it may be because you are behind a 
firewall and have not configured your build tool to use your HTTP proxy.  Please 
refer to the following information.)
    Ant users:     http://ant.apache.org/manual-beta/proxy.html
    Maven users:   http://maven.apache.org/guides/mini/guide-proxies.html


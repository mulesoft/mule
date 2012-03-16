+-------------------------------------+
| Adding Message Processors to a Flow |
+-------------------------------------+
This example shows how to add message processing components, in this example Logger and Echo, to a Flow. By doing so, you can perform custom logging in your Mule application. Logging is useful to introspect the current message and create logging events for your specific needs.

We introduce several new concepts here:

1. Message Processor
2. Mule Expressions

For more information, refer to 
http://www.mulesoft.org/documentation/display/MULE3EXAMPLES/Adding+Message+Processors+to+a+Flow

+----------------------------------------+
| Running the example in Mule Standalone |
+----------------------------------------+
Simply copy the pre-built application archive (mule-example-adding-logging-to-a-flow.zip) to the application folder ($MULE_HOME/apps) and start Mule. 

To access the webapps go to

    http://localhost:8084/<your_message>

Mule app will return <your_message> back, additionally will log your message in the application log ($MULE_HOME/logs/mule-echo.log).

+------------------------------------------+
| Building the example for Mule Standalone |
+------------------------------------------+
First, make sure you have set the MULE_HOME environment variable as recommended in Mule's README.txt

You can build the example by simply running "mvn".  This will compile the 
example classes and produce a zip file that will be copied into the application 
folder ($MULE_HOME/apps).

+---------------------------------------+
| Building the example for a Web Server |
+---------------------------------------+
You can build the adding-logging-to-a-flow webapp as a fully deployable .WAR archive by running "mvn compile war:war".  This will produce a adding-logging-to-a-flow.war archive in the target directory which you can deploy to any JEE web server such as Tomcat, Jetty, JBoss, etc.

The .WAR archive contains all needed Mule libraries and the following line
inside the web.xml file which will start up Mule when the webapp is deployed:

<listener-class>org.mule.config.builders.MuleXmlBuilderContextListener</listener-class>

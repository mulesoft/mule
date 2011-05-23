+---------------------+
| Stock Quote Example |
+---------------------+
This example demonstrates how to invoke an ASPX web service from Mule and
transform the result using XSLT and deserialise the result to a StockQuote
JavaBean. The example demonstrates using REST and SOAP to invoke the service.

For more information, refer 
http://www.mulesoft.org/documentation/display/MULE3EXAMPLES/Stock+Quote+Example

+---------------------+
| Running the example |
+---------------------+
Simply copy the pre-built application archive to the application folder
($MULE_HOME/apps) and start Mule. To access the web service
invoke one of the following URIs from your browser:

    http://localhost:48309/stockquote?symbol=CSCO&method=REST
    http://localhost:48309/stockquote?symbol=CSCO&method=SOAP
    http://localhost:48309/stockquote?symbol=CSCO&method=WSDL

+-------------------+
| Firewall settings |
+-------------------+
If you are behind a firewall, you will need to configure the settings for your
HTTP proxy in the proxy.properties file. Go to the unpacked application inside
the application folder ($MULE_HOME/apps). Put your proxy configuration in
proxy.properties (located in the classes folder) and uncomment the proxy
settings section in mule-config.xml. Mule will automatically redeploy the
example and activate the config changes.

+----------------------+
| Building the example |
+----------------------+
Depending on the build tool you are using (Ant or Maven), you can build the
example by simply running "ant" or "mvn".  This will compile the example
classes, produce an application zip file, and copy everything to 
$MULE_HOME/apps.

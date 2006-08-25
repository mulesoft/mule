+---------------------+
| Stock Quote Example |
+---------------------+
This example demonstrates how to invoke an ASPX web service from Mule and transform the
result using XSLT and deserialise the result to a StockQuote javabean. The example
demonstrates using REST and SOAP to invoke the service.

For more information, refer to http://www.muleumo.org/Examples

If you haven't yet added Mule to your executable path, please follow the instructions
in INSTALL.txt first.  To run this example:

Linux / Unix
------------
mule -config ./conf/soap-config.xml
mule -config ./conf/wsdl-config.xml
mule -config ./conf/rest-config.xml

Windows
-------
mule.bat -config .\conf\soap-config.xml
mule.bat -config .\conf\wsdl-config.xml
mule.bat -config .\conf\rest-config.xml

+-------------------+
| Firewall settings |
+-------------------+

If you are behind a firewall, you will need to configure the settings for your HTTP proxy
in the proxy.properties file.  Then you can run the example as follows:

Linux / Unix
------------
mule -config ./conf/soap-proxy-config.xml -props proxy.properties
mule -config ./conf/wsdl-proxy-config.xml -props proxy.properties
mule -config ./conf/rest-proxy-config.xml -props proxy.properties

Windows
-------
mule.bat -config .\conf\soap-proxy-config.xml -props proxy.properties
mule.bat -config .\conf\wsdl-proxy-config.xml -props proxy.properties
mule.bat -config .\conf\rest-proxy-config.xml -props proxy.properties

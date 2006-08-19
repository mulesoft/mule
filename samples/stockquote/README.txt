+---------------------+
| Stock Quote Example |
+---------------------+

This example demonstrates how to invoke an ASPX web service from Mule and transform the result using 
XSLT and deserialise the result to a StockQuote javabean. The example demonstrates using REST and SOAP 
to invoke the service.


To run this example:

Linux / Unix
------------
mule -config soap-config.xml
mule -config wsdl-config.xml
mule -config rest-config.xml

Windows
-------
mule.bat -config soap-config.xml
mule.bat -config wsdl-config.xml
mule.bat -config rest-config.xml

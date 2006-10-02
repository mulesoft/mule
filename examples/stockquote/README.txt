+---------------------+
| Stock Quote Example |
+---------------------+
This example demonstrates how to invoke an ASPX web service from Mule and transform the
result using XSLT and deserialise the result to a StockQuote javabean. The example
demonstrates using REST and SOAP to invoke the service.

For more information, refer to http://mule.mulesource.org/Examples

+---------------------+
| Running the example |
+---------------------+
Simply use the shell script (Unix/Linux) or batch file (Windows) provided in this directory to run 
the example.

Alternatively, if you have added Mule to your executable path as recommended in INSTALL.txt, you 
can run the example from the command line as follows:

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
in the ./conf/proxy.properties file.  Then you can run the example as follows:

    Linux / Unix
    ------------
    mule -config ./conf/soap-config.xml -props ./conf/proxy.properties
    mule -config ./conf/wsdl-config.xml -props ./conf/proxy.properties
    mule -config ./conf/rest-config.xml -props ./conf/proxy.properties

    Windows
    -------
    mule.bat -config .\conf\soap-config.xml -props .\conf\proxy.properties
    mule.bat -config .\conf\wsdl-config.xml -props .\conf\proxy.properties
    mule.bat -config .\conf\rest-config.xml -props .\conf\proxy.properties

+----------------------+
| Building the example |
+----------------------+
First, make sure you have set the MULE_HOME environment variable as recommended in INSTALL.txt

Depending on the build tool you are using (Ant or Maven), you can build the example by simply 
running "ant" or "mvn".  This will compile the example classes, produce a jar file, and copy 
everything to $MULE_HOME/lib/user, which is where your custom classes and configuration files 
should go.  

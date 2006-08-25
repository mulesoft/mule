+---------------------+
| Hello World Example |
+---------------------+
This sample uses two components to create a hello world message. When the sample starts
it prompts the user at the console to type in their name, the user's name is then passed
to the first component which adds something to the string before passes on to the second
component that also adds some text before outputting the results back to the console.

For more information, refer to http://www.muleumo.org/Examples

If you haven't yet added Mule to your executable path, please follow the instructions
in INSTALL.txt first.  To run this example:

Linux / Unix
------------
mule -config ./conf/hello-config.xml
mule -config ./conf/hello-http-config.xml
mule -config ./conf/hello-spring-config.xml -builder spring

Windows
-------
mule.bat -config .\conf\hello-config.xml
mule.bat -config .\conf\hello-http-config.xml
mule.bat -config .\conf\hello-spring-config.xml -builder spring


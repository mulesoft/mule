+---------------------+
| Hello World Example |
+---------------------+
This example uses two components to create a hello world message. When the example starts
it prompts the user at the console to type in their name, the user's name is then passed
to the first component which adds something to the string before passes on to the second
component that also adds some text before outputting the results back to the console.

For more information, refer to http://www.muleumo.org/Examples

If you haven't yet added Mule to your executable path, please follow the instructions
in INSTALL.txt first.  To run this example:

mule -config conf/hello-config.xml
mule -config conf/hello-http-config.xml
mule -config conf/hello-spring-config.xml -builder spring


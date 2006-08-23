+-----------------------+
| Error Handler Example |
+-----------------------+
The Error handler sample demonstrates using Spring as the external container to provide
the objects managed by Mule and how to publish events to multiple outbound endpoints.
The sample consists of two components: ExecptionManager and BusinessErrorManager.

For more information, refer to http://www.muleumo.org/Examples

If you haven't yet added Mule to your executable path, please follow the instructions
in INSTALL.txt first.  To run this example:

Linux / Unix
------------
mule -config error-config.xml
mule -config error-spring-config.xml -builder spring

Windows
-------
mule.bat -config error-config.xml
mule.bat -config error-spring-config.xml -builder spring

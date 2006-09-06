+-----------------------+
| Error Handler Example |
+-----------------------+
The Error handler example demonstrates using Spring as the external container to provide
the objects managed by Mule and how to publish events to multiple outbound endpoints.
The example consists of two components: ExecptionManager and BusinessErrorManager.

For more information, refer to http://www.muleumo.org/Examples

If you haven't yet added Mule to your executable path, please follow the instructions
in INSTALL.txt first.  To run this example:

Linux / Unix
------------
mule -config ./conf/error-config.xml

Windows
-------
mule.bat -config .\conf\error-config.xml

+-----------------+
| E-mail settings |
+-----------------+

The FatalException part of this example sends an alert e-mail to the Mule administrator.
For this to work, you will need to configure your e-mail address and SMTP settings in the
file ./conf/email.properties  Then you can run the example as follows:

Linux / Unix
------------
mule -config ./conf/error-config.xml -props ./conf/email.properties

Windows
-------
mule.bat -config .\conf\error-config.xml -props .\conf\email.properties

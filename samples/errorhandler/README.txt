+-----------------------+
| Error Handler Example |
+-----------------------+

The Error handler sample demonstrates using Spring as the external container to provide the objects 
managed by Mule and how to publish events to multiple outbound endpoints. The sample consists of two 
components; ExecptionManager and BusinessErrorManager.


To run this example:

Linux / Unix
------------
mule -config error-config.xml

Windows
-------
mule.bat -config error-config.xml

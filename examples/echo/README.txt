+--------------+
| Echo Example |
+--------------+
This example demonstrates how to expose a Mule component over multiple transports,
in this case as an Axis web sevice and via System.in (request) and System.out (response).

For more information, refer to http://mule.mulesource.org/wiki/display/MULE/Examples

+---------------------+
| Running the example |
+---------------------+
Simply use the shell script (Unix/Linux) or batch file (Windows) provided in this directory to run 
the example.

Alternatively, if you have added Mule to your executable path as recommended in INSTALL.txt, you 
can run the example from the command line as follows:

    Linux / Unix
    ------------
    mule -config ./conf/echo-config.xml

    Windows
    -------
    mule.bat -config .\conf\echo-config.xml

+----------------------+
| Building the example |
+----------------------+
This example has no custom classes so there's not much point in building it.

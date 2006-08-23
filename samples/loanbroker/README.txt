+---------------------+
| Loan Broker Example |
+---------------------+
The application design is based on the Loan Broker example presented in the Enterprise
Integration Patterns book.

This example demonstrates the Loan Broker without using a shared message bus, but rather
an Enterprise Service Network approach (no shared message type and components interacting
directly).  The example demonstrates Synchronous and Asynchronous message processing
styles.

For more information, refer to http://www.muleumo.org/Examples

If you haven't yet added Mule to your executable path, please follow the instructions
in INSTALL.txt first.  To run this example:

Linux / Unix
------------
mule -main org.mule.samples.loanbroker.LoanConsumer

Windows
-------
mule.bat -main org.mule.samples.loanbroker.LoanConsumer

+-----------------------+
| JMS Example |
+-----------------------+
The JMS example demonstrates some JMS test cases using ActiveMQ.

For more information, refer to http://mule.mulesource.org/Examples

+----------------------+
| Building the example |
+----------------------+
First, make sure you have set the MULE_HOME environment variable as recommended in INSTALL.txt

Depending on the build tool you are using (Ant or Maven), you can build the example by
simply running "ant" or "mvn".  This will download any additional libraries, compile the
example classes, produce a jar file, and copy everything to $MULE_HOME/lib/user, which is
where your custom classes and configuration files should go.

(If you are unable to download the libraries it may be because you are behind a firewall
and have not configured your build tool to use your HTTP proxy.  Please refer to the
following information.)
    Ant users:     http://ant.apache.org/manual-beta/proxy.html
    Maven users:   http://maven.apache.org/guides/mini/guide-proxies.html

+---------------------+
| Running the example |
+---------------------+
In a shall window, use the shell script (Unix/Linux) or batch file (Windows) provided
in this directory to run the example.

Alternatively, if you have added Mule to your executable path as recommended in
INSTALL.txt, you can run the example from the command line as follows:

    Linux / Unix
    ------------
    mule -config ./conf/jms-config.xml

    Windows
    -------
    mule.bat -config .\conf\jms-config.xml

When you start the example, Mule will use the Quartz Scheduler Provider to generate some test JMS messages. Each message is a TextMessage type of JMS Message. The messages differ in both payload (different text content) and in the header properties. The first kind of message has a property called "function" that is set to "ItemResponse". The second kind of message has this property set to "TestItemResponse". Both messages generate every 10 seconds, with a 2 second gap between each.

The first <mule-descriptor> route defines two different output JMS queues to which the different messages are routed. Each <outbound-router> definition uses the MessagePropertyFilter to check the "function" property. The first outbound router will send the first kind of message to the JMS queue named "first.queue" while the second will send the second kind of message to the queue named "second.queue".

The next two <mule-descriptor> routes listen for messages coming from those two JMS queues. Messages from the queue named "first.queue" will go to the console. Messages from the queue named "second.queue" will be written as files in the "output" directory (this directory will be automatically created).

To stop the test, type "CTRL-C".


The MC4J plugin is a work in progress.
It provides some customised view for manageing aMule instance
so far the following views are provided

1. Mule Server View - a Global dashboard show the instance information and configuration and memory usage.
2. Component Stats View - A list of the registered components with most important statistics. Stats update every 3 seconds.
3. All Stats page - (currently not working) shows ful stats information about the mule server (similar t Jdmk Stats view)
4. Server Notifications - (Currently Not working) Displays all notification events fired by the server. Updated every 2 seconds
5. Component Graphical Stats - Right-clicking on a Component MBean view you can Select a Graphical stats view

Prerequisites
=============
1. Need to have MC4j 1.2b9 installed (see http://mc4j.org)
2. Enable your Mule instance for remote Jmx management (See http://mule.codehaus.org/Jmx+Management)
   Note from Mule 1.3 onwards you can use the Jmx Default configuration by registering the following Jmx Agent -
   <agents>
        <agent name="JMX" className="org.mule.management.agents.DefaultJmxSupportAgent"/>
   </agents>
   This will register Jmx remoting on the default URI: service:jmx:rmi:///jndi/rmi://localhost:1099/server

To Install The plug-in
======================
1. Run 'maven' in this directory.
2. Unzip the generated distribution (under target/mule-tools-mc4j-xxx-dist.zip) into the root of you're MC4J inatannlation.
3. Start Your Mule server(s) (Ensure you have the Jmx Agents registered in your Mule Xml)
4. Start MC4J
5. Set up a new connection for each of your servers.  If you've used the default Mule Jmx settings the connection URI
   will be: service:jmx:rmi:///jndi/rmi://localhost:1099/server

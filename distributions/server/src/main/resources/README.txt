GETTING STARTED WITH MULE
=========================

This file describes how to get started using Mule. For full details,
see the Mule User Guide at:
http://mule.mulesource.org/display/MULE2USER/Home


Setting Up Your Environment
===========================

Before you can use Mule, you must create the MULE_HOME environment
variable and set it to the location of your Mule installation
(Throughout the Mule documentation, this directory is referred
to as MULE_HOME). You must also add the location of your
MULE_HOME/bin directory to your path. If you are using Windows,
you can use the System utility in the Control Panel to add the
MULE_HOME variable and edit your path. Alternatively, you can
use the export or set commands (depending on your operating system)
at the command prompt, as shown in the following examples:

Linux/Unix:
    export MULE_HOME=/opt/mule
    export PATH=$PATH:$MULE_HOME/bin

Windows:
    set MULE_HOME=C:\Mule
    set PATH=%PATH%;%MULE_HOME%\bin 


Distribution Contents
=====================

The Mule distribution contains the following directories and files:

/bin            Shell and batch scripts for controlling Mule from the command line
/conf           Configuration files
/docs           API documentation (Javadoc) for Mule and its sub-projects
/examples       Example applications you can run and try building yourself
/lib/boot       Libraries used by the Java Service Wrapper to boot the server
/lib/endorsed   Endorsed Java libraries used by Mule
/lib/mule       Mule libraries
/lib/opt        Third-party libraries
/lib/user       Your custom classes and libraries. This directory comes before
                /lib/mule on the classpath and can be used to patch the distributed 
                Mule classes. You must restart Mule after adding files to this directory.
/licences       License information for all libraries shipped with Mule
/logs           Log file output when running in background mode
/sbin           Internal scripts (not to be run by the user)
/src            The source code for all Mule modules
LICENSE.txt     License agreement for Mule
README.txt      The Getting Started document you are reading


Running Mule
============

Now that you have installed Mule, you are ready to get started!
This section describes how to run Mule.


Starting with the Examples
--------------------------

To run Mule, you must specify a configuration file to use. Typically,
this is an XML file called mule-config.xml. The examples directory
provides you with several examples of Mule applications including
their configuration files, which you can use as the starting point
for creating your configuration file. For more information,
see http://mule.mulesource.org/display/MULEINTRO/Examples.


Working with Configuration Files
--------------------------------

If needed, you can specify more than one configuration file in a
comma-separated list. This approach is useful for splitting up
your Mule configuration to make it more manageable. All configuration
files must be on the classpath prior to startup. A convenient way
to achieve this is by placing them in the /conf or /lib/user directory
Alternatively, you can specify an explicit path to their location on
the file system. If you make changes to a configuration file, you must
restart Mule for the changes to take effect.


Using the Command Prompt
------------------------

To run Mule, you enter the following command at the command prompt:

    mule [-config your-config.xml]

where your-config.xml is the Mule XML configuration file you want to use.
This command runs Mule in the foreground. To stop Mule, enter Ctrl-C.

To run Mule in the background as a daemon, enter the following command
instead, using start, stop, or restart as the first parameter as needed:

    mule start|stop|restart [-config <your-config.xml>]

For more information on running Mule, see: 
http://www.mulesource.org/display/MULEINTRO/Starting+the+Server.


Additional Setup for Community Edition Users
--------------------------------------------

If you are running Mule Community Edition, you take a few extra
steps before you can use Mule.

- License Display and Acceptance

When you start Mule for the first time, the MuleSource Public
License is displayed page by page. To advance a page, press Enter.
At the end of the license display, type y to accept the license
file and proceed with startup.


Testing the Installation
------------------------

After Mule has started successfully, it displays "Mule Server 
initialized." The default port for the Mule AdminAgent is 60504.
To test the connection, open another window and type the following
command to search for port 60504:

Windows Console:    netstat -an|find "60504"
Unix Command Shell: netstat -an|grep 60504


Basic Usage
===========

There are three general types of tasks you can perform to configure
and customize your Mule deployment:

- Service component development: developing POJOs, services, or beans
that contain the business logic and will be used as service components
in a Mule deployment.

- Integration: developing routers, transformers, and filters, and configuring
everything in the Mule configuration file.

- Extending Mule: developing new transports, connectors, and other modules
used by Mule.

This section provides a high-level overview of the steps you take to
perform these tasks.


Create a Service Component
--------------------------

A service component is a class, web service, or other application that
contains the business logic you want to plug in to the Mule framework.
You can use any existing application, or create a new one. Your service
component does not need to contain any Mule-specific code. All the
Mule-specific instructions will be configured for the service that
wraps the service component.

To assist development, you can use the Mule IDE, an Eclipse plug-in
that provides an integrated development environment for developing
Mule applications. You can download the Mule IDE from the MuleForge at:
http://www.mulesource.org/display/IDE/Home

If you want more advanced information on writing service components,
see: http://mule.mulesource.org/display/MULE2USER/Working+with+Services


Configure the Service
---------------------

You create a service definition in the Mule configuration file that
points to the service component, routers, filters, and transformers.
It also specifies the endpoint on which this service will receive
messages and the outbound endpoint where messages will go next.
For more information, see:
http://mule.mulesource.org/display/MULE2USER/Configuring+the+Service

Following is more information on configuring routers, filters,
and transformers for the service.

- Routers

Inbound routers specify how messages are routed to a service,
and outbound routers specify how messages are routed after the
service has finished processing them. There are several default
routers that come with Mule that you can use, or you can create
your own routers. For more information, see:
http://mule.mulesource.org/display/MULE2USER/Using+Message+Routers

- Filters

Filters specify conditions that must be met for a message to be
routed to a service. There are several default filters that come
with Mule that you can use, or you can create your own filters.
For more information, see:
http://mule.mulesource.org/display/MULE2USER/Using+Filters

- Transformers

Transformers convert incoming payload data to the type required
by the service component. After the service has finished processing
the message, they can also convert the message to a different type
as needed by the outbound transport. There are several default
transformers you can use, or create your own. For more information, see:
http://mule.mulesource.org/display/MULE2USER/Using+Transformers


Configure Mule
--------------

Each Mule instance has one Registry. The Registry acts as a
runtime registry of all the Mule components, including models, agents,
connectors, endpoints, and transformers. You configure the server URL
for the Mule instance, the working directory where it writes temporary
files, and more. For more information, see:
http://mule.mulesource.org/display/MULE2USER/Configuration+Overview


Extend Mule
-----------

Mule provides transports for many different channels, including File,
FTP, HTTP, JMS, JDBC, Quartz, and many more. There are also community-
created transports on MuleForge (http://muleforge.org/). If you need
to send messages on a channel other than those provided, you can
create a new transport. You can also create a custom connector for
a transport. A connector is the Java class in the transport that
contains the actual logic for sending and receiving messages on
that channel. For more information, see:
http://mule.mulesource.org/display/MULE2USER/About+Transports

For information on using the Mule project wizard to create new
Mule projects (transports and other types of modules), see:
http://mule.mulesource.org/display/MULE2USER/Tools


Where Do I Go Next?
===================

This document has provided a brief overview of getting started
with Mule. Following is information about where to go next.

- For complete information on using Mule, go to the Mule User Guide at:
http://www.mulesource.org/display/MULE2USER/Home.

- If you need assistance and are a Mule Enterprise customer,
see the support page at: http://www.mulesource.org/display/MULE/Support

- If you are evaluating Mule and want to find out about subscription
options, you can submit a request for MuleSource to contact you by
going to http://www.mulesource.com/buynow/, or call us at 877-MULE-OSS.

- All Mule users can subscribe to the Mule mailing lists. You can find
these lists at http://mule.mulesource.org/display/MULE/Mailing+Lists

- If you experience problems with the Mule software or documentation,
please log an issue in the MuleSource issue-tracking system, located at:
http://mule.mulesource.org/jira/browse/MULE

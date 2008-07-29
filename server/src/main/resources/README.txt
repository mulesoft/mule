GETTING STARTED WITH MULE
=========================

This file describes the basics of how to get started using Mule. 
For full deta ils,see the Mule Getting Started Guide at:
http://www.mulesource.org/display/MULE2INTRO/Home


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
/licenses       License information for all libraries shipped with Mule
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
see http://www.mulesource.org/display/MULE2INTRO/Examples.


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

If you are running Mule Community Edition, the MuleSource Public
License is displayed page by page when you start Mule for the first time. 
To advance a page, press Enter. At the end of the license display, 
type y to accept the license file and proceed with startup.


Where Do I Go Next?
===================

This document has provided a brief overview of getting started
with Mule. Following is information about where to go next.

- For information on more ways to run Mule and how to get started using
the examples, go to the Getting Started Guide at: 
http://www.mulesource.org/display/MULE2INTRO/Home 

- For complete information on using Mule, go to the Mule User Guide at:
http://www.mulesource.org/display/MULE2USER/Home. You will need to register
to view these pages, but registration is free and only takes a few moments.

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
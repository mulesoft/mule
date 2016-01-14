GETTING STARTED WITH MULE
=========================

This file describes the basics of how to get started using Mule.
For full details, see https://developer.mulesoft.com/docs/display/current/Installing.


Setting Up Your Environment
===========================

Mule uses the MULE_HOME environment variable to point to the
location of your Mule installation.  It is good practice to
set this variable in your environment.  However, if it is not
set at startup, Mule will temporarily set it based on the location
of the startup script.

You may also want to add the MULE_HOME/bin directory to your path.
If you are using Windows, you can use the System utility in the
Control Panel to add the MULE_HOME variable and edit your path.
Alternatively, you can use the export or set commands (depending
on your operating system) at the command prompt, as shown in the
following examples:

Linux/Unix:
    export MULE_HOME=/opt/mule
    export PATH=$PATH:$MULE_HOME/bin

Windows:
    set MULE_HOME=C:\Mule
    set PATH=%PATH%;%MULE_HOME%\bin


Distribution Contents
=====================

The Mule distribution contains the following directories and files:

/bin            Shell and batch scripts for controlling Mule from the command
                line
/conf           Configuration files
/docs           API documentation (Javadoc) for Mule and its sub-projects
/lib/boot       Libraries used by the Java Service Wrapper to boot the server
/lib/endorsed   Endorsed Java libraries used by Mule
/lib/mule       Mule libraries
/lib/opt        Third-party libraries
/lib/user       Your custom classes and libraries. This directory comes before
                /lib/mule on the classpath and can be used to patch the
                distributed Mule classes. You must restart Mule after adding
                files to this directory.
/logs           Log file output when running in background mode
/src            The source code for all Mule modules
LICENSE.txt     License agreement for Mule
README.txt      The Getting Started document you are reading


Running Mule
============



Starting with the Examples
--------------------------

Up-to-date Mule Runtime examples can be found in:

- MuleSoft documentation - http://developer.mulesoft.com/docs
- Anypoint Studio - downloadable from https://www.mulesoft.com/platform/studio
- Anypoint Exchange - https://anypoint.mulesoft.com/exchange/

From the Exchange, you can also access the direct URL for the example that you can download using a utility like wget.


Using the Command Prompt
------------------------

To run Mule, enter the following command at the command prompt:

    mule

To stop Mule, enter:

    Ctrl-C

To run Mule in the background as a daemon, enter the following command
using start, stop, or restart as the first parameter as needed.

    mule start|stop|restart

For more information on running Mule, see
https://developer.mulesoft.com/docs/display/current/Downloading+and+Starting+Mule+ESB.


Where Do I Go Next?
===================

This document has provided a brief overview of getting started with Mule. What follows is information about where to go next.

- For complete information on using Mule, go to the Mule User Guide at:
https://developer.mulesoft.com/docs/display/current/Home

- As a Mule Enterprise user, if you need assistance see the support page at
https://www.mulesoft.com/support-and-services/mule-esb-support-license-subscription

- If you are evaluating Mule and want to find out about subscription
options, you can submit a request for MuleSoft to contact you by
going to https://www.mulesoft.com/platform/soa/mule-esb-open-source-esb.

- If you experience problems with the Mule software or documentation,
please log an issue in the MuleSoft issue-tracking system, located at
http://www.mulesoft.org/jira/browse/MULE

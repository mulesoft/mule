/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

import org.mule.util.SystemUtils

//------------------------------------------
// Configuration parameters
// Update if necessary
//------------------------------------------

// default value
exampleLauncherAppUrl = "http://localhost:18082/examples/"

// list of examples that should be deployed as part of the launcher example application
enabledExamples = ['mule-example-launcher', 'hello', 'echo', 'stockquote', 'scripting', 'bookstore', 'gpswalker']

//------------------------------------------

muleHome = SystemUtils.getenv()['MULE_HOME']
exampleAppsBaseDir = new File("${muleHome}/examples")
deployDir = new File("${muleHome}/apps")

antBuilder = new AntBuilder()

LF_CR = System.getProperty("line.separator")
TAB = "   "

// validate command line arguments & directories
if(args?.size() > 1)
{
    usage()
    System.exit(1)
}
else if(args?.size() == 1)
{
    exampleLauncherAppUrl = args[0]
}

if (!exampleAppsBaseDir.isDirectory())
{
    println "${muleHome} is not a valid MULE_HOME or missing required folder ${exampleAppsBaseDir}."
    System.exit(2)
}

// deploy examples
def files = exampleAppsBaseDir.listFiles()
for(currentFile in files)
{
    // examples are organized in directories
    if(currentFile.isDirectory())
    {
        deployExample(currentFile)
    }
}

// start Mule
splash "Starting Mule ESB..."

def command = SystemUtils.IS_OS_WINDOWS ? "cmd /c start mule.bat" : "./mule start"
def proc = Runtime.getRuntime().exec(command, null, new File("${muleHome}/bin/"))
if(!SystemUtils.IS_OS_WINDOWS)
{
    proc.waitFor()
    println proc.text
}


// wait for the example launcher application to be started
splash "Waiting for example applications to become ready..."
def i = 0
def timeoutInMs = 300000 // 3 minutes
def waitIntervalInMs = 3000
def loops = timeoutInMs / waitIntervalInMs

while(!isExampleLauncherApplicationReady() && i < loops)
{
    print "."
    sleep(waitIntervalInMs)
    i++
}

print LF_CR

if(i < loops)
{
    // launch browser
    splash "Example launcher application is up and running.${LF_CR}${TAB}${TAB}Goto ${exampleLauncherAppUrl} to launch examples!"
    System.exit(0)
}
else
{
   // timeout waiting for example launcher application to be ready
   splash "Timeout waiting for Mule ESB to be ready.${LF_CR}${TAB}${TAB}Goto ${exampleLauncherAppUrl} to launch examples!"
   System.exit(3)
}



/**
    Print usage information.
*/
def usage()
{
    println '''

Start Mule ESB with example applications deployed and ready to try

Usage: mule_examples [example launcher index URL]

'''
}

/**
    Checks if the example should be deployed and if so, deploy it
*/
def deployExample(exampleDir)
{
    if(enabledExamples.contains(exampleDir.name))
    {
        def exampleFiles = exampleDir.listFiles().grep(~/.*zip$/)
        // this should be really one
        for(exampleFile in exampleFiles)
        {
            if(exampleFile.isFile() && !isExampleDeployed(exampleFile))
            {
                // deploy (copy) application file to apps directory
                splash "Deploying ${exampleFile.name}"
                antBuilder.copy(file: exampleFile.getCanonicalPath(), tofile: "${deployDir}/${exampleFile.name}")
            }
        }
    }
}

/**
    Checks if the example represented by the file is already deployed
*/
def isExampleDeployed(exampleFile)
{
    def applicationName = exampleFile?.name?.lastIndexOf('.') >= 0 ? exampleFile.name[0 .. exampleFile?.name?.lastIndexOf('.') - 1] : exampleFile?.name

    return (new File("${deployDir}/${applicationName}")).exists()
}

/**
    Checks if the example launcher application is up and ready to handle requests
*/
def isExampleLauncherApplicationReady()
{
    def connection
    try
    {
        def urlInfo = exampleLauncherAppUrl.toURL()
        connection = urlInfo.openConnection()

        // connection object is lazy... forcing connection
        connection.responseCode

        return true
    }
    catch(ConnectException ce)
    {
        // jetty is still down
        return false
    }
    finally
    {
        connection?.disconnect()
    }
}

/**
    A helper splash message method.
*/
def splash(text) {
    println()
    println '=' * 62
    println "${TAB}$text"
    println '=' * 62
}

/**
 * SwitchVersion
 *
 * Finds recursively all pom.xml files and switches <old version> to <new version>.
 * 
 * $Id$
 */

import java.util.regex.Pattern

def cliBuilder = new CliBuilder()
cliBuilder.f(longOpt: "from", args: 1, required: true, "switch from version (e.g. 2.0)")
cliBuilder.h(longOpt: "help", "show usage info")
cliBuilder.r(longOpt: "root", args: 1, required: true, "start scanning at this root folder")
cliBuilder.t(longOpt: "to", args: 1, required: true, "switch to version (e.g. 2.1)")

options = cliBuilder.parse(args)
if (!options)
{
    println ""
    println "Error parsing options " + args
    println ""
    System.exit(1)
}
if (options.h)
{
    cliBuilder.usage()
    System.exit(0)
}

def root = new File(options.r)
oldVersion = options.f
newVersion = options.t

versionPattern = Pattern.compile("\\s*<version>.*</version>.*")

root.eachFileRecurse()
{ file ->

    if (file.directory == false)
    {
        if (file.name == 'pom.xml')
        {
            process(file)
        }
        else if (file.name == 'install.xml')
        {
            // MULE-2659: take care of the installer configuration file as well
            switchAppVersion(file)
        }
    }
}

//-----------------------------------------------------------------------------
def process(input)
//-----------------------------------------------------------------------------
{
    println("processing " + input)

    def outputFile = new File(input.getParent(), "pom.xml.new")
    def output = new BufferedWriter(new FileWriter(outputFile))

    def processingParentElement = false
    def versionProcessed = false

    input.eachLine
    { line ->

        if (line.indexOf("<parent>") > -1)
        {
            processingParentElement = true
        }
        else if (line.indexOf("</parent>") > -1)
        {
            processingParentElement = false
        }
        def versionMatch = versionPattern.matcher(line).matches();

        if (processingParentElement && versionMatch)
        {
            outputLine(output, switchVersion(line, "version", oldVersion, newVersion))
        }
        else
        {
            if ((versionProcessed == false) && versionMatch)
            {
                versionProcessed = true
                outputLine(output, switchVersion(line, "version", oldVersion, newVersion))
            }
            else
            {
                if (line.indexOf("<muleVersion>") > -1)
                {
                    outputLine(output, switchVersion(line, "muleVersion", oldVersion, newVersion))
                }
                else
                {
                    outputLine(output, line)
                }
            }
        }
    }

    output.close()

    // rename the new pom back to 'pom.xml'
    def backupFile = new File(input.getParent(), "pom.xml.orig")
    input.renameTo(backupFile)

    outputFile.renameTo(input)

    // remove the backup file
    backupFile.delete()
}

//-----------------------------------------------------------------------------
def switchAppVersion(installerConfigFile)
//-----------------------------------------------------------------------------
{
    println("processing " + installerConfigFile)

    def outputFile = new File(installerConfigFile.getParent(), "install.xml.new")
    def output = new BufferedWriter(new FileWriter(outputFile))

    installerConfigFile.eachLine
    { line ->

        if (line.indexOf("<appversion>") > -1)
        {
            outputLine(output, switchVersion(line, "appversion", oldVersion, newVersion))
        }
        else
        {
            outputLine(output, line)
        }
    }

    output.close()

    def backupFile = new File(installerConfigFile.getParent(), "install.xml.orig")
    installerConfigFile.renameTo(backupFile)
    outputFile.renameTo(installerConfigFile)
    backupFile.delete()
}

//-----------------------------------------------------------------------------
def switchVersion(line, versionTag, oldVersion, newVersion)
//-----------------------------------------------------------------------------
{
    startVersionTag = "<" + versionTag + ">"
    endVersionTag = "</" + versionTag + ">"

    // include the tag length
    int start = line.indexOf(startVersionTag) + startVersionTag.length();
    int end = line.indexOf(endVersionTag);
    def version = line.substring(start, end);

    if ((version == oldVersion) == false)
    {
        return line;
    }

    StringBuffer processedLine = new StringBuffer(128);
    processedLine.append(line.substring(0, start));
    processedLine.append(newVersion);
    processedLine.append(line.substring(end));
    return processedLine.toString();
}

//-----------------------------------------------------------------------------
def outputLine(output, line)
//-----------------------------------------------------------------------------
{
    output.write(line)
    output.newLine()
}

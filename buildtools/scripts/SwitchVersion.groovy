/**
 * SwitchVersion
 *
 * Usage: SwitchVersion <directory> <old version> <new version>
 *
 * Finds recursively all pom.xml files and switches <old version>
 * to <new version>.
 */

import java.util.regex.Pattern

def root = new File(args[0])

oldVersion = args[1]
newVersion = args[2]

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

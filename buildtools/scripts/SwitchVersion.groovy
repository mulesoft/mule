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

    if ((file.directory == false) && (file.name == 'pom.xml'))
    {
        process(file)
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
            outputLine(output, switchVersion(line, oldVersion, newVersion))
        }
        else
        {
            if ((versionProcessed == false) && versionMatch)
            {
                versionProcessed = true
                outputLine(output, switchVersion(line, oldVersion, newVersion))
            }
            else
            {
                outputLine(output, line)
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
def switchVersion(line, oldVersion, newVersion)
//-----------------------------------------------------------------------------
{
    // include the tag length
    int start = line.indexOf("<version>") + 9;
    int end = line.indexOf("</version>");
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

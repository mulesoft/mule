/**
 * This script helps switching the version in all Schemas and XML config files
 * 
 * $Id$
 */

import org.codehaus.groovy.ant.FileScanner

def root = "."

sourceSchemaVersion = "2.0"
destSchemaVersion = "2.1"
schemaBase = "http://www.mulesource.org/schema/mule/"

if (args.length > 0)
{
    root = args[0]
}

//
// first pass: convert all schema files
//
AntBuilder ant = new AntBuilder()
FileScanner scanner = ant.fileScanner 
{
    fileset(dir: root) 
    {
        include(name: "**/*.xsd")
        include(name: "**/*.xml")
        exclude(name: "**/pom.xml")
        exclude(name: "**/target/**")
        exclude(name: "**/test-data/out/**")
    }
}
scanner.each 
{
    println "switching schema version on $it"
    switchSchemaVersion(it)
}

def switchSchemaVersion(File inFile)
{    
    def filename = inFile.name + ".version-switched"
    def outFile = new File(inFile.parentFile, filename)
    def outWriter = new PrintWriter(new FileWriter(outFile))
    
    inFile.eachLine
    {
        line ->

        def schemaName = findSchemaName(line)
        if (schemaName != null)
        {
            updateSchema(line, schemaName, outWriter)            
        }
        else
        {
            outWriter.println(line)
        }
    }
    
    outWriter.close()

    def backupFile = new File(inFile.parentFile, inFile.name + ".bak")
    move(inFile, backupFile)
    move(outFile, inFile)
}

def findSchemaName(String line)
{
    def schemaBaseIndex = line.indexOf(schemaBase)
    if (schemaBaseIndex == -1)
    {
        return null
    }
    
    if (line.indexOf(sourceSchemaVersion) == -1)
    {
        return null
    }
        
    def startSearchIndex = schemaBaseIndex + schemaBase.length()
    def schemaNameEndIndex = line.indexOf("/", startSearchIndex)
    return line.substring(startSearchIndex, schemaNameEndIndex)
}

def updateSchema(String line, String schemaName, PrintWriter writer)
{    
    def urlBase = schemaBase + schemaName + "/"
    def originalUrl = urlBase + sourceSchemaVersion
    def replacementUrl = urlBase + destSchemaVersion

    writer.println(line.replaceAll(originalUrl, replacementUrl))
}

def move(File sourceFile, File destFile)
{
    if (destFile.exists())
    {
        throw new IOException(destFile.canonicalPath + " exists!")
    }

    
    if (sourceFile.renameTo(destFile) == false)
    {
        fail("moving " + sourceFile.absolutePath + " to " + destFile.absolutePath + " failed")
    }
}

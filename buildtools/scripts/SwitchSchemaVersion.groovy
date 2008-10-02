/**
 * This script helps switching the version in all Schemas and XML config files
 * 
 * $Id$
 */

import org.codehaus.groovy.ant.FileScanner

def root = "."

sourceSchemaVersion = "2.0"
destSchemaVersion = "2.1"
schemaBases = [ "http://www.mulesource.org/schema/mule/", "http://www.mulesource.com/schema/mule/" ]

if (args.length > 0)
{
    root = args[0]
}

// switch the version in all XSD and XML files
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

def noChange = {
    return it   
}

scanner.each 
{
    //println "switching schema version on $it"
    switchSchemaVersion(it, noChange, noChange)
}

// switch all spring.handlers and spring.schemas files
scanner = ant.fileScanner
{
    fileset(dir: root)
    {
        include(name: "**/spring.handlers")
        include(name: "**/spring.schemas")
        exclude(name: "**/target/**")
    }
}

def preProcess = {
    return it.replace("\\:", ":")
}

def postProcess = {
    return it.replace(":", "\\:")
}

scanner.each
{
    //println "switching schema version on $it"
    switchSchemaVersion(it, preProcess, postProcess)
}

def switchSchemaVersion(File inFile, def preProcessFunction, def postProcessFunction)
{    
    def filename = inFile.name + ".version-switched"
    def outFile = new File(inFile.parentFile, filename)

    def schemaMatched = false
    outFile.withWriter
    {
        outWriter ->
             
        inFile.eachLine
        {
            line ->
    
            def preprocessed = preProcessFunction(line)

            def matchingSchemaBase = findMatchingSchema(preprocessed)
            if (matchingSchemaBase != null)
            {
                schemaMatched = true
                
                def schemaName = findSchemaName(preprocessed, matchingSchemaBase)
                def convertedLine = updateSchema(preprocessed, matchingSchemaBase, schemaName)
                
                convertedLine = postProcessFunction(convertedLine)
                
                outWriter.writeLine(convertedLine)
            }
            else
            {
                outWriter.writeLine(preprocessed)
            }
        }
    }    
    
    if (schemaMatched)
    {
        def backupFile = new File(inFile.parentFile, inFile.name + ".bak")
        move(inFile, backupFile)
        move(outFile, inFile)
    }
    else
    {
        delete(outFile)
    }
}

def findMatchingSchema(String line)
{
    for (String schema : schemaBases)
    {
        if ((line.indexOf(schema) > -1) && (line.indexOf(sourceSchemaVersion) >= -1))
        {
            return schema
        }
    }
    
    return null
}

def findSchemaName(String line, String schemaBase)
{
    def schemaBaseIndex = line.indexOf(schemaBase)
    if (schemaBaseIndex == -1)
    {
        throw new IllegalArgumentException("$schemaBase not found in $line")
    }
            
    def startSearchIndex = schemaBaseIndex + schemaBase.length()
    def schemaNameEndIndex = line.indexOf("/", startSearchIndex)
    return line.substring(startSearchIndex, schemaNameEndIndex)
}

def updateSchema(String inputLine, String schemaBase, String schemaName)
{    
    def urlBase = schemaBase + schemaName + "/"
    def originalUrl = urlBase + sourceSchemaVersion
    def replacementUrl = urlBase + destSchemaVersion
    
    return inputLine.replace(originalUrl, replacementUrl)
}

def move(File sourceFile, File destFile)
{
    if (destFile.exists())
    {
        throw new IOException(destFile.canonicalPath + " exists!")
    }
    
    if (sourceFile.renameTo(destFile) == false)
    {
        throw new IOException("moving " + sourceFile.absolutePath + " to " + destFile.absolutePath + " failed")
    }
}

def delete(File file)
{
    if (file.delete() == false)
    {
        throw new IOException("deleting " + file.canonicalFile + " failed")
    }
}

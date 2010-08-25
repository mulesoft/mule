/**
 * This script helps switching the version in all Schemas and XML config files
 * 
 * $Id$
 */

import org.codehaus.groovy.ant.FileScanner

def cliBuilder = new CliBuilder()
cliBuilder.f(longOpt: "from", args: 1, "switch from version (e.g. 2.2)")
cliBuilder.h(longOpt: "help", "show usage info")
cliBuilder.r(longOpt: "root", args: 1, "start scanning at this root folder")
cliBuilder.t(longOpt: "to", args: 1, "switch to version (e.g. 3.0)")

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

def root = "."
if (options.r)
{
    root = options.r
}    

sourceSchemaVersion = "2.2"
if (options.f)
{
    sourceSchemaVersion = options.f
}

destSchemaVersion = "3.0"
if (options.t)
{
    destSchemaVersion = options.t
}

// this regex matches both, the CE (.org) and EE (.com) schema in the source version
// There is one matching group around the whole schema so the matching schema name can be
// accessed from the code below
schemaRegex = /(http:\/\/www.mulesoft.[org|com]{3}?\/schema\/mule[\/\w+\/]+$sourceSchemaVersion)/
xsdRegex = /(http:\/\/www.mulesoft.[org|com]{3}?\/schema\/mule[\/\w+\/]+$sourceSchemaVersion\/.*xsd)/
//
// switch the version in all XSD and XML files
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

def noChange = {
    return it   
}

scanner.each 
{
    println "switching schema version on $it"
    switchSchemaVersion(it, noChange, noChange, xsdRegex)
    switchSchemaVersion(it, noChange, noChange, schemaRegex)
}

//
// switch all spring.handlers and spring.schemas files
//
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
    println "switching schema version on $it"
    switchSchemaVersion(it, preProcess, postProcess, xsdRegex)
    switchSchemaVersion(it, preProcess, postProcess, schemaRegex)
}

def switchSchemaVersion(File inFile, def preProcessFunction, def postProcessFunction, def matcher)
{    
    def filename = inFile.name + ".version-switched"
    def outFile = new File(inFile.parentFile, filename)

    def schemaReplaced = false
    outFile.withWriter
    {
        outWriter ->
             
        inFile.eachLine
        {
            line ->
    
            line = preProcessFunction(line)
            
            def match = (line =~ matcher)
            while (match.find())
            {
                schemaReplaced = true
                
                def srcSchema = null
                srcSchema = match[0][1]
                def destSchema = null
                if(match =~ "xsd")
                {
                    destSchema = srcSchema.replace(sourceSchemaVersion, destSchemaVersion)
                }
                else
                {
                    destSchema = srcSchema.replace(sourceSchemaVersion, "")
                    //strip last '/'
                    destSchema = destSchema.substring(0,destSchema.length()-1)
                }
                line = line.replace(srcSchema, destSchema)
                
                match = (line =~ matcher)
            }
            
            def convertedLine = postProcessFunction(line)
            outWriter.writeLine(convertedLine)
        }
    }    
    
    if (schemaReplaced)
    {
        def backupFile = new File(inFile.parentFile, inFile.name + ".bak")
        move(inFile, backupFile)
        move(outFile, inFile)
        delete(backupFile)
    }
    else
    {
        delete(outFile)
    }
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

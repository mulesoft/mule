/*
 * Genrerates a file that can be imported as eclipse XML catalog
 *
 * $Id$
 */

import org.codehaus.groovy.ant.FileScanner

//  the structure of xsd locations
corexsd = /.*(\/|\\)spring-config(\/|\\).*(\/|\\)mule.xsd/
coreEnterpriseXsd = /.*(\/|\\)spring-config(\/|\\).*(\/|\\)mule-ee.xsd/
otherxsd = /.*(\/|\\)(transports|modules|tests)(\/|\\)([^\/]+)(\/|\\).*(\/|\\)mule-(.*)\.xsd/

def cliBuilder = new CliBuilder()
cliBuilder.a(longOpt: "absolute","use absolute filenames instead of workspace references")
cliBuilder.c(longOpt: "community", "use community schema URLs")
cliBuilder.e(longOpt: "enterprise", "use enterprise schema URLs")
cliBuilder.h(longOpt: "help", "show usage info")
cliBuilder.r(longOpt: "root", required: true, args: 1, "start scanning at this root folder")
cliBuilder.s(longOpt: "schemaVersion", args: 1, "use the specified version")

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

root = options.r

base = "http://www.mulesoft.org/schema/mule/"
if (options.e)
{
    base = "http://www.mulesoft.org/schema/mule/ee/"
}

schemaVersion = "3.4"
if (options.s)
{
    schemaVersion = options.s
}

ant = new AntBuilder()
searchEclipseProjects()

def searchEclipseProjects()
{
    nameRegEx = /.*<name>(.*)<\/name>.*/

    println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">")

    FileScanner scanner = ant.fileScanner
    {
        fileset(dir:root)
        {
            include(name:"**/.project")
        }
    }
    scanner.each
    {
        projectFile ->

        // extract the project's name from the .project file
        match = (projectFile.getText() =~ nameRegEx)
        projectName =  match[0][1]

        processProject(projectFile, projectName)
    }

    println("</catalog>")
}

def processProject(projectFile, projectName)
{
    projectDir = projectFile.getParentFile()

    FileScanner scanner = ant.fileScanner
    {
        fileset(dir:projectDir)
        {
            include(name:"**/*.xsd")
            exclude(name:"**/src/test/**/*.xsd")
            exclude(name:"**/target/**/*.xsd")
        }
    }
    scanner.each
    {
        printSchemaEntry(projectName, projectFile, it)
    }
}

def printSchemaEntry(projectName, projectFile, xsdFile)
{
    schemaSource = ""
    if (xsdFile.absolutePath ==~ corexsd)
    {
        schemaSource = "${base}core/$schemaVersion/mule.xsd"
    }
    else if (options.e && xsdFile.absolutePath ==~ coreEnterpriseXsd)
    {
        schemaSource = "${base}core/$schemaVersion/mule-ee.xsd"
    }
    else if (xsdFile.absolutePath ==~ otherxsd)
    {
        match = (xsdFile.absolutePath =~ otherxsd)
        name = match[0][7]

        urlPath = name
        if (options.e)
        {
            // The name still has the -ee suffix. The URLs however may not include the -ee suffix
            urlPath = name.replace("-ee", "")
        }

        schemaSource = "${base}${urlPath}/$schemaVersion/mule-${name}.xsd"
    }
    else
    {
         println "WARNING: ignoring $xsdFile"
         return
    }

    projectDir = projectFile.parentFile
    dirLength = projectDir.absolutePath.length()
    schemaRelativePath = xsdFile.absolutePath.substring(dirLength + 1)

    print("  <uri name=\"${schemaSource}\" ")

    if (options.a)
    {
        // TODO check if this is correct notation on Windows - I doubt so
        print("uri=\"file://${projectDir}/${schemaRelativePath}")
    }
    else
    {
        print("uri=\"platform:/resource/${projectName}/${schemaRelativePath}")
    }
    println("\"/>")
}

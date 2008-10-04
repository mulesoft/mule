/*
 * Genrerates a file that can be imported as eclipse XML catalog
 */

//  the structure of xsd locations
corexsd = /.*(\/|\\)spring-config(\/|\\).*(\/|\\)mule.xsd/
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

base = "http://www.mulesource.org/schema/mule/"
if (options.e)
{
    base = "http://www.mulesource.com/schema/mule/"
}

schemaVersion = "2.2"
if (options.s)
{
    schemaVersion = options.s
}

//checkCurrentDirectory()
searchEclipseProjects()

def checkCurrentDirectory()
{
    if (! (new File("").getCanonicalFile().getName() == "scripts"))
    {
        println ""
        println "WARNING: run from in the buildtools/scripts directory"
        println ""
        System.exit(1)
    }
}

def searchEclipseProjects()
{
    nameRegEx = /.*<name>(.*)<\/name>.*/
    
    println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">")
    
    for (projectFile in new AntBuilder().fileScanner {
        fileset(dir:root)
        {
            include(name:"**/.project")
        }})
    {
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
    for (xsdFile in new AntBuilder().fileScanner {
        fileset(dir:projectDir)
        {
            include(name:"**/*.xsd")
            exclude(name:"**/src/test/**/*.xsd")
            exclude(name:"**/target/**/*.xsd")
        }})
    {
        printSchemaEntry(projectName, projectFile, xsdFile)
    }
}

def printSchemaEntry(projectName, projectFile, xsdFile)
{
    schemaSource = ""
    if (xsdFile.absolutePath ==~ corexsd)
    {
        schemaSource = "${base}core/$schemaVersion/mule.xsd"
    }
    else if (xsdFile.absolutePath ==~ otherxsd)
    {
        match = (xsdFile.absolutePath =~ otherxsd)
        name = match[0][7]
        schemaSource = "${base}${name}/$schemaVersion/mule-${name}.xsd"
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

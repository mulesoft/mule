/*
 * Genrerates a file that can be imported as eclipse XML catalog
 */

//assume we are running in the buildtools/scripts directory
root = "../.."

//  the structure of xsd locations
corexsd = /.*(\/|\\)core(\/|\\).*(\/|\\)mule.xsd/
otherxsd = /.*(\/|\\)(transports|modules)(\/|\\)([^\/]+)(\/|\\).*(\/|\\)mule-(.*)\.xsd/

//destination base
base = "http://www.mulesource.org/schema/mule/"

def checkCurrentDirectory()
{
	if (! (new File("").getCanonicalFile().getName() == "scripts")) 
	{
		println ""
		println "WARNING: run from in the scripts directory"
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
	    schemaSource = "${base}core/2.0/mule.xsd"
	}
	else if (xsdFile.absolutePath ==~ otherxsd) 
	{
		match = (xsdFile.absolutePath =~ otherxsd)
		name = match[0][7]
		schemaSource = "${base}${name}/2.0/mule-${name}.xsd"
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
	print("uri=\"platform:/resource/${projectName}/${schemaRelativePath}")
	println("\"/>")
}

checkCurrentDirectory()
searchEclipseProjects()

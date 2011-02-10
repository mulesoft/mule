/*
 * XmlCatalogFromMuleStandaloneDistro - generate an xml catalog suitable
 * for importing into Eclipse from a local Mule installation.
 *
 * $Id$
 */

import org.codehaus.groovy.ant.FileScanner
import java.util.jar.JarFile
import java.util.jar.JarEntry

if (args.length != 1)
{
    usage()
}

ant = new AntBuilder()
FileScanner scanner = ant.fileScanner
{
    File libDir = new File(args[0], "lib/mule")
    fileset(dir: libDir)
    {
        include(name: "**/*.jar")
    }
}

println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>")
println("<catalog xmlns=\"urn:oasis:names:tc:entity:xmlns:xml:catalog\">")

scanner.each
{
    file ->

    printSchemaEntry(file)
}

println("</catalog>")

def usage()
{
    println("XmlCatalogFromMuleStandaloneDistro <absolute path to Mule distribution>")
    System.exit(1)
}

def printSchemaEntry(File file)
{
    Properties springSchemasProperties = loadSpringSchemasProperties(file)
    if (springSchemasProperties != null)
    {
        String unixPath = file.getAbsolutePath().replace("\\", "/")

        springSchemasProperties.each
        {
            key, value ->

            println("<uri name=\"$key\" uri=\"jar:file:/$unixPath!/$value\"/>")
        }
    }
}

def loadSpringSchemasProperties(File file)
{
    Properties props = null

    JarFile jar = new JarFile(file)
    JarEntry springSchemasEntry = jar.getEntry("META-INF/spring.schemas")
    if (springSchemasEntry != null)
    {
        InputStream input = jar.getInputStream(springSchemasEntry)

        props = new Properties()
        props.load(input)

        input.close()
    }
    jar.close()

    return props
}

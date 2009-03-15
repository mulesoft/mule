/**
 * Recursively scan through all java files checking the file header
 */
public class ScanLicenseHeaders
{
    /**
     * These files are known to have invalid license headers and are the usual exception to the rule
     */
    static List exceptions = [ "BobberArchetype.java", "BobberArchetypeMojo.java", 
        "ExampleArchetypeMojo.java", "ProjectArchetypeMojo.java", "ModuleArchetypeMojo.java",
        "TransportArchetypeMojo.java", "XMLStreamReaderToContentHandler.java", "BndMojo.java" ];

    static void main(args)
    {
        if (args.length != 1)
        {
            println("usage: ScanLicenseHeaders <path>")
            System.exit(1)
        }

        if (scan(new File(args[0])) == false)
        {
            System.exit(1)
        }
    }

    static boolean scan(File scanRoot)
    {
        def retValue = true;

        def ant = new AntBuilder()
        def scanner = ant.fileScanner {
            fileset (dir: scanRoot) {
                include(name: "**/*.java")
                exclude(name: "**/target/**")
            }
        }

        def licenseLine = "The software in this package is published under the terms of the CPAL v1.0";

        scanner.each { file ->

            file.withReader { reader ->

                // using the standard file header, the license is in line 6
                def line = null
                6.times {
                    line = reader.readLine()
                }

                if (line.indexOf(licenseLine) == -1)
                {
                    if (exceptions.contains(file.name) == false)
                    {
                        println("License suspect: $file")
                        retValue = false
                    }
                }
            }
        }

        return retValue
    }
}


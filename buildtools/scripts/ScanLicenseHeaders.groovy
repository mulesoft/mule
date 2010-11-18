/**
 * Recursively scan through all java files checking the file header
 * 
 * $Id$
 */

import java.io.File;

public class ScanLicenseHeaders
{
    /**
     * Files in the following packages are known do have invalid license headers
     */
    static List ignoredPackages = [ "net/webservicex", "org/hibernate"  ]

    /**
     * These files are known to have invalid license headers and are the usual exception to the rule
     */
    static List ignoredFiles = [ "BndMojo.java", "BobberArchetype.java", "BobberArchetypeMojo.java",
        "ConfigurationPatternArchetypeMojo.java", "DummySSLServerSocketFactory.java", "ExampleArchetypeMojo.java", "ModuleArchetypeMojo.java",
        "MultipartConfiguration.java", "Part.java", "ProjectArchetypeMojo.java",
        "TransportArchetypeMojo.java", "XMLStreamReaderToContentHandler.java" ];

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
        def ant = new AntBuilder()
        def scanner = ant.fileScanner {
            fileset (dir: scanRoot) {
                include(name: "**/*.java")
                exclude(name: "**/target/**")
            }
        }

        scanner.each { file ->
            return scanFile(file)
        }
    }

    static boolean scanFile(File file)
    {
        def licenseLine = "The software in this package is published under the terms of the CPAL v1.0";

        file.withReader { reader ->

            // using the standard file header, the license is in line 6
            def line = null
            6.times {
                line = reader.readLine()
            }

            // no line? Most probably this file doesn't even have a license header
            if (line == null)
            {
                return false
            }

            if (line.indexOf(licenseLine) == -1)
            {
                if (isInIgnoredPackage(file))
                {
                    return true
                }

                if (ignoredFiles.contains(file.name) == false)
                {
                    println("License suspect: $file")
                    return false
                }
            }
        }

        return true
    }

    static boolean isInIgnoredPackage(File file)
    {
        def folder = file.getParent();

        for (String pkg : ignoredPackages)
        {
            if (folder.endsWith(pkg))
            {
                return true;
            }
        }

        return false
    }
}

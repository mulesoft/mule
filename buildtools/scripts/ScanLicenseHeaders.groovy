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
    static boolean fail = false
    /**
     * These files are known to have invalid license headers and are the usual exception to the rule
     */
    static List ignoredFiles = [ "ClassReader.java",
        "DummySSLServerSocketFactory.java",
        "MultipartConfiguration.java", "ParamReader.java", "Part.java",
        "XMLStreamReaderToContentHandler.java",
        "__artifactId__IBean.java", "__artifactId__IBeanTestCase.java"];

    static void main(args)
    {
        if (args.length != 1)
        {
            println("usage: ScanLicenseHeaders <path>")
            System.exit(1)
        }

        scan(new File(args[0]))
        
        if(fail)
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
                fail = true
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
                    fail = true
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
            if (folder.contains(pkg))
            {
                return true;
            }
        }

        return false
    }
}

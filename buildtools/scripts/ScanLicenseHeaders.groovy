/**
 * Recursively scan through all java files checking the file header
 *
 * $Id$
 */

// These files are known to have invalid license headers and are the usual exception to the rule
exceptions = [ ];

// these lines must be present on the file header
licenseLines = [
        "This software is protected under international copyright",
        "law. All use of this software is subject to MuleSoft's Master Subscription Agreement",
        "(or other master license agreement) separately entered into in writing between you and",
        "MuleSoft. If such an agreement is not in place, you may not use the software."
]

if (args.length != 1)
{
    println("usage: ScanLicenseHeaders <path>")
    System.exit(1)
}

if (scan(new File(args[0])) == false)
{
    System.exit(1)
}

def scan(File scanRoot)
{
    def ant = new AntBuilder()
    def scanner = ant.fileScanner
            {
                fileset (dir: scanRoot)
                        {
                            include(name: "**/*.java")
                            exclude(name: "**/target/**")
                        }
            }

    def retValue = true;
    scanner.each
            { file ->

                def didPrint = false
                file.withReader
                        { reader ->

                            // using the standard file header, the license is in line 6
                            def line = null
                            5.times {
                                line = reader.readLine()
                            }

                            if (!exceptions.contains(file.name))
                            {
                                for (String license : licenseLines)
                                {
                                    if (line.indexOf(license) == -1)
                                    {
                                        if (!didPrint)
                                        {
                                            println("License suspect: $file")
                                            didPrint = true
                                        }
                                        retValue = false
                                    }

                                    line = reader.readLine()
                                }
                            }
                        }
            }

    return retValue
}


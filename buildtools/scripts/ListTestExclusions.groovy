/**
    Recursively scan for mule-test-exclusions.txt files and report excluded tests for each project.
    Runs from the currend directory, work dir is Mule project root.

    $Id$
*/

def parser = new XmlSlurper()

def cliBuilder = new CliBuilder()
cliBuilder.r(longOpt: "root", args: 1, required: true, "start scanning at this root folder")

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

def muleRoot = options.r

def ant = new AntBuilder()

def scanner = ant.fileScanner
{
    fileset (dir: muleRoot)
    {
        include(name: "**/src/test/resources/mule-test-exclusions.txt")
    }
}

def lines = []

scanner.each
{
    file -> file.eachLine()
    {
        line -> if (!line.startsWith('#') && line.length() != 0)
        {
            lines << line
        }
    }
}

lines.sort()

lines.each
{
    line -> println(line)
}


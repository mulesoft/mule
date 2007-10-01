/**
    Recursively scan for mule-test-exclusions.txt files and report excluded tests for each project.
    Runs from the currend directory, work dir is Mule project root.

    $Id$
*/

def muleRoot = '../..'

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


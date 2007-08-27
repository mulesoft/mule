/**
 * Recursively scan through all java files checking the file header
 */
def muleRoot = '../..'

def ant = new AntBuilder()
def scanner = ant.fileScanner {
    fileset (dir: muleRoot) {
        include(name: "**/*.java")
    }
}

def licenseLine = "The software in this package is published under the terms of the MuleSource MPL";

scanner.each { file ->
    def reader = new BufferedReader(file.newReader())

    // using the standard file header, the license is in line 5
    def line = null
    for (i in 0..5)
    {
        line = reader.readLine()
    }

    if (line.indexOf(licenseLine) == -1)
    {
        println("check file " + file)
    }
}

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
            println("License suspect: $file")
        }
    }
}

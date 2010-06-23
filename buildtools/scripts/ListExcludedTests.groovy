/**
    Recursively scan for pom.xml files and report excluded tests for each project.
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
def scanner = ant.fileScanner {
    fileset (dir: muleRoot) {
        include(name: "**/pom.xml")
    }
}

scanner.each { file ->
    def project = parser.parse(file)
    printExcludes(project)
}

/**
    Analyze Surefire configuration if available and print excluded tests.
*/
def printExcludes(project) {
    def plugins = project?.build?.plugins?.plugin.findAll { it.artifactId == 'maven-surefire-plugin' }
    plugins.each { plugin ->
        splash "$project.name"
        plugin?.configuration?.excludes.exclude.each {
            println it
        }
    }
}

/**
    A helper splash message method.
*/
def splash(text) {
    println()
    println '=' * 50
    println "  $text"
    println '=' * 50
}

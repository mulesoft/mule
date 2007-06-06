/**
    Recursively scan for pom.xml files and report excluded tests for each project.
    Runs from the currend directory, work dir is Mule project root.

    $Id$
*/
def muleRoot = '../..'
def parser = new XmlSlurper()

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
/**
    Recursively scan for pom.xml files and report excluded tests for each project.
    Run from the Mule project root for best results.

    $Id$
*/
def parser = new XmlSlurper()

new File('.').eachFileRecurse { file ->
    if (!file.directory && file.name == 'pom.xml') {
        def project = parser.parse(file)
        printExcludes(project)
    }
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
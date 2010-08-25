/**
    The report scans all modules for testcases which are actually executed
    by the build. Information from module poms is incorporated to reflect
    excluded tests.
    
    Runs from the currend directory, work dir is Mule project root.

    $Id: ListExcludedTests.groovy 5406 2007-03-04 17:58:50Z aperepel $
*/

def muleRoot = '../..'


def parser = new XmlSlurper()
def testCount = 0
def ant = new AntBuilder()

new File(muleRoot).eachFileRecurse { file ->
    if (!file.directory && file.name == 'pom.xml') {
        def project = parser.parse(file)
        def testNames = []
        
        splash project.name

        // custom tests directory locations not supported at the moment
        def testsDir = new File(file.parent, 'src/test/java/')
        
        // ugly way to break out of closure, continue not supported here        
        if (testsDir.exists()) {
            
            // TODO fetch these from a top level pom
            def topLevelExcludes = [
                            '**/Abstract*TestCase.java',
                            '**/target/**'
                            ]
            
            def moduleExcludes = []

            // scan module pom to extract local excludes
            def plugins = project?.build?.plugins?.plugin.findAll { it.artifactId == 'maven-surefire-plugin' }
            plugins.each { plugin ->
                plugin?.configuration?.excludes.exclude.each {
                    moduleExcludes << it
                }
            }
                            
            def scanner = ant.fileScanner {
                fileset(dir: testsDir) {
                    include(name: '**/*TestCase.java')
                    // from the root pom
                    topLevelExcludes.each { e ->
                        exclude(name: e)
                    }
                    // from each individual pom
                    moduleExcludes.each { e ->
                        exclude(name: e)
                    }
                }
            }
            
            scanner.each { test ->
                testNames << test.name
            }
            
            // print all with index and sorted
            testNames.sort().eachWithIndex { name, i ->
                println "${(i + 1).toString().padLeft(3)}. ${name}"
            }
            
            testCount += testNames.size()
        }
    }
}

splash "Total included test count: $testCount"


/**
    A helper splash message method.
*/
def splash(text) {
    println()
    println '=' * 50
    println "  $text"
    println '=' * 50
}

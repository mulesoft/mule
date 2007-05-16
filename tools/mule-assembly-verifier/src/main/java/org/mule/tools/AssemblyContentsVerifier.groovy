package org.mule.tools

import org.codehaus.mojo.groovy.GroovyMojoSupport
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.project.MavenProject

/**
 * @goal verify
 */
class AssemblyContentsVerifier extends GroovyMojoSupport
{
    /**
     * The file to touch.
     *
     * @parameter default-value="src/main/resources/assembly-whitelist.txt"
     */
    File whitelist

    /**
     * File name which contents will be verified.
     * @parameter default-value="${project.build.finalName}.${project.packaging}"
     */
    String projectOutputFile

    /**
     * Project instance.
     * @parameter default-value="${project}"
     * @required
     * @reeadonly
     */
    MavenProject project

    void execute() {
        // sanity check
        if (!whitelist.exists()) {
            throw new MojoExecutionException("Whitelist file $whitelist does not exist.")
        }

        // splash
        log.info "*" * 80
        log.info("Verifying contents of the assembly".center(80))
        log.info "*" * 80

        // confirm output file is available
        def outputFile = new File("$project.build.directory/$projectOutputFile")
        if (!outputFile.exists()) {
            throw new MojoExecutionException("Output file $outputFile does not exist.")
        }

        // temp directory to unpack to
        def tempDir = new File("${project.build.directory}/mule-assembly-verifier-temp")

        ant.unzip(src: outputFile,
                  dest: tempDir)

        // list all jars
        def jars = []
        tempDir.eachFileRecurse() { file ->
            if (!file.directory && file.name ==~ /.*\.jar/) {
                jars << file.name
            }
        }

        log.debug("Jars in the assembly: $jars")

        def muleVersion = "$project.version"
        // strip version and jar suffixes
        def actualNames = jars.collect {
            if (it ==~ /^mule.*/) {
                // for Mule libs
                return it - "-${muleVersion}.jar"
            } else {
                // for 3rd-party libs
                return it - ".jar"
            }
        }

        log.debug("Filtered jar names: $actualNames")

        def missing = findMissing(actualNames)
        def unexpected = findUnexpected(actualNames)

        if (missing || unexpected) {
            def msg = new StringBuffer("The following problems have been encountered:\n\n")
            if (missing) {
                msg << "\tMissing libraries:\n"
                missing.eachWithIndex { name, i ->
                    msg << "\t\t\t${(i + 1).toString().padLeft(3)}. ${name}\n"
                }
            }
            if (unexpected) {
                msg << "\tUnexpected libraries:\n"
                unexpected.eachWithIndex { name, i ->
                    msg << "\t\t\t${(i + 1).toString().padLeft(3)}. ${name}\n"
                }
            }
            throw new MojoFailureException(msg as String)
        }
    }

    def findMissing(actualNames) {
        // load the whitelist
        def expected = []
        whitelist.eachLine() { expected << it }
        log.debug("Whitelist: $expected")
        // find all whitelist entries which are missing
        expected.findAll {
            !actualNames.contains(it)
        }.sort { it.toLowerCase() } // sort case-insensitive
    }

    def findUnexpected(actualNames) {
        // load the whitelist
        def expected = []
        whitelist.eachLine() { expected << it }
        log.debug("Whitelist: $expected")
        // find all libs not in the whitelist
        actualNames.findAll {
            !expected.contains(it)
        }.sort { it.toLowerCase() } // sort case-insensitive
    }
}

/*
 * Run an archetype and compile it using maven
 *
 * $Id$
 */

import org.apache.commons.io.FileUtils
import org.apache.commons.lang.SystemUtils

/*
 * Make sure that the archetype can do its job, i.e. remove any leftovers from
 * the last invocation of the archetype
 */
def buildDir = new File(project.build.directory)

if (project.properties.outputDir == null)
{
    fail("Specify a property 'outputDir' in the config section of the groovy-maven-plugin that invokes this script")
}
def existingProjectDir = new File(project.build.directory, project.properties.outputDir)

if (existingProjectDir.exists())
{
    FileUtils.forceDelete(existingProjectDir)
}

// make sure that the output dir is created before the actual Maven run that follows now
existingProjectDir.mkdirs()

/*
 * run Maven archetype
 */
def cmdline = "-o "

if (project.properties.archetype == null)
{
    fail("Specify the archetype to be invoked via a property named 'archetype' in the config section of the groovy-maven-plugin that invokes this script")
}
def archetype = project.properties.archetype
if (archetype.endsWith(":") == false)
{
    archetype += ":"
}
cmdline += archetype + project.version + ":create "

if (project.properties.archetypeParams != null)
{
    cmdline += project.properties.archetypeParams
}
cmdline += " -DmuleVersion=" + project.version
cmdline += " -Dinteractive=false"
runMaven(cmdline, buildDir)

// now that the source is generated, compile it using Maven
// Do not run "mvn test" here since the generated source is not testable as is
cmdline = "test-compile"
runMaven(cmdline, existingProjectDir)

def runMaven(String commandline, File directory)
{
    def maven = "mvn"
    if (SystemUtils.IS_OS_WINDOWS)
    {
        maven = "mvn.bat"
    }
    commandline = maven + " " + commandline

    log.info("***** commandline: '" + commandline + "'")

    // null means inherit parent's env ...
    def process = commandline.execute(null, directory)

    // consume all output of the forked process. Otherwise it may lock up
    process.in.eachLine { log.info(it) }
    process.err.eachLine { log.error(it) }

    process.waitFor()
    def exitCode = process.exitValue()
    if (exitCode != 0)
    {
        fail("command did not execute properly: " + exitCode)
    }
}

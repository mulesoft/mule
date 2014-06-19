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

if (project.properties.skipArchetypeTests == "true")
{
    log.info("Skipping tests")
    return
}

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

// determine the local repository to use. Users may have specified a different local
// repo when invoking the original Maven bulid (-Dmaven.repo.local=xxx). Of course
// we need to use the same local repository on the forked Maven invocation
localRepoArgument = "-Dmaven.repo.local=${settings.localRepository}"

// get Maven archetype name
if (project.properties.archetype == null)
{
    fail("Specify the archetype to be invoked via a property named 'archetype' in the config section of the groovy-maven-plugin that invokes this script")
}
def archetype = project.properties.archetype
if (archetype.endsWith(":") == false)
{
    archetype += ":"
}

// run archetype on build dir
def cmdline = buildArchetypeCommand(archetype, 'create', project.properties.archetypeParams)
runMaven(cmdline, buildDir)

// if a pre-compilation archetype call has been configured, run it in the generated project
def preCompileArchetypeGoal = project.properties.preCompileArchetypeGoal
if (preCompileArchetypeGoal != null)
{
  cmdline = buildArchetypeCommand(archetype, preCompileArchetypeGoal, project.properties.preCompileArchetypeParams)
  runMaven(cmdline, existingProjectDir)
}

// now that the source is generated, compile it using Maven
// do not run "mvn test" here since the generated source is not testable as is
cmdline = "${localRepoArgument} test-compile"
runMaven(cmdline, existingProjectDir)

def buildArchetypeCommand(String archetype, String goal, String archetypeParams)
{
    def cmdline = "-o ${localRepoArgument} "
    cmdline += archetype + project.version + ":${goal} "

    if (archetypeParams != null)
    {
        cmdline += archetypeParams
    }
    cmdline += ' -DmuleVersion=' + project.version
    cmdline += ' -Dinteractive=false'
}

def runMaven(String commandline, File directory)
{
    def maven = SystemUtils.IS_OS_WINDOWS ? "mvn.bat" : "mvn"

    def mavenHome = System.getenv("MAVEN_HOME")
    if (mavenHome != null)
    {
        mavenHome = new File(mavenHome, "bin");
        maven = new File(mavenHome, maven).getAbsolutePath()
    }

    commandline = maven + " " + commandline

    log.info("***** directory: '${directory}'")
    log.info("***** commandline: '${commandline}'")

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

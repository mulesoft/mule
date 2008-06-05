
import org.apache.commons.io.FileUtils

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

/*
 * run Maven archetype
 */
def cmdline = "mvn -o "

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

log.info("***** commandline: '" + cmdline + "'")

// null means inherit parent's env ...
def process = cmdline.execute(null, buildDir)

// consume all output of the forked process. Otherwise it won't run.
def input = new BufferedReader(new InputStreamReader(process.inputStream))
input.eachLine { log.info(it) }
process.waitFor()

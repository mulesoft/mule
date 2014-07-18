/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
import java.util.jar.JarEntry
import java.util.jar.JarFile
import groovy.transform.Field
import groovy.transform.AutoClone

@Field String GET_PLUGIN = "org.apache.maven.plugins:maven-dependency-plugin:2.8:get"
@Field long FIVE_MINUTES = 300000
@FIELD int INVALID_ARGUMENTS = 1
@Field String help = '\nDeploys to a remote Maven repository Mule CE and EE artifacts including distributions, poms, jars, test jars, javadoc jars and source jars.\n'
@Field String ceRepoId
@Field String ceRepoUrl
@Field Boolean deployDistro
@Field String version
@Field String m2repo
@Field packagingExceptions = ['geomail': 'war']
@Field deployExceptions = ['archetypes', 'integration-axis', 'loanbroker-legacy']
@Field artifactIdExceptions = ['jboss-transactions'      : 'mule-module-jbossts',
                               'bobberplus'              : 'bobberplus',
                               'mule-transport-archetype': 'mule-transport-archetype',
                               'mule-project-archetype'  : 'mule-project-archetype',
                               'mule-module-archetype'   : 'mule-module-archetype',
                               'mule-example-archetype'  : 'mule-example-archetype',
                               'mule-catalog-archetype'  : 'mule-catalog-archetype']

parseArguments(args)
deployJars()
if (deployDistro) { deployCeDistributions(version) }

def parseArguments(def arguments)
{
    def options = parseOptions(arguments)
    if (!options || options.h)
    {
        log(help)
        System.exit(INVALID_ARGUMENTS)
    }
    version = options.v
    m2repo = options.r
    deployDistro = options.d ? true : false
    ceRepoId = options.ce.split('::')[0]
    ceRepoUrl = options.ce.split('::')[1]
}

private OptionAccessor parseOptions(arguments)
{
    def cliBuilder = new CliBuilder(usage: 'deploy_mule_artifacts [options]\n\n')
    cliBuilder.r(longOpt: "local-repository", required: true, args: 1, "Maven 2 repository to get artifacts from.")
    cliBuilder.v(longOpt: "version", required: true, args: 1, "Mule ESB version")
    cliBuilder.h(longOpt: "help", "show usage info")
    cliBuilder.d(longOpt: "deploy-distributions", "Deploy Distribution Artifacts")
    cliBuilder.ce(longOpt: "ce-repository", required: true, args: 1, "CE Remote repository (id::repository)")
    return cliBuilder.parse(arguments)
}

def deployJars()
{
    deployToRemote(ceRepoUrl, ceRepoId, 'org.mule', 'mule', version, 'pom')
    deployToRemote(ceRepoUrl, ceRepoId, 'org.mule', 'mule-core', version, 'jar')
    [
            'examples',
            'modules',
            'patterns',
            'tests',
            'tools',
            'transports',
    ].each {
        def pom = new Artifact(groupId: 'org.mule.' + it, artifactId: 'mule-' + it, version: version, packaging: 'pom')
        assert getDependency(pom, 'target/pom')
        deployToRemote(ceRepoUrl, ceRepoId, pom.groupId, pom.artifactId, version, 'pom')
        def project = new XmlSlurper().parse('target/pom')
        project.modules.children().each { module ->
            if (module.text().startsWith('all-'))
            {
                deployToRemote(ceRepoUrl, ceRepoId, pom.groupId, "mule-${it}-all", version, 'pom')
            }
            else
            {
                String group = ['tests', 'tools'].contains(it) ? it : it.minus(~/s$/)
                String packaging = it.equals('examples') ? 'zip' : 'jar'
                packaging = packagingExceptions.get(module.text()) ? packagingExceptions.get(module.text()) : packaging
                String artifact = "mule-${group}-${module.text()}"
                artifact = artifactIdExceptions.get(module.text()) ? artifactIdExceptions.get(module.text()) : artifact
                if (deployExceptions.contains(module.text()))
                {
                    return
                }
                deployToRemote(ceRepoUrl, ceRepoId, pom.groupId, artifact, version, packaging)
            }
        }
    }
}

protected void deployToRemote(String repoUrl, String repoId, String groupId, String artifactId, String version, String packaging, String classifier = null)
{
    Artifact pom = new Artifact(groupId: groupId, artifactId: artifactId, version: version, packaging: 'pom')
    Artifact pomSignature = pom.having(packaging: 'pom.asc')
    Artifact artifact = pom.having(packaging: packaging, classifier: classifier)
    Artifact signature = artifact.having(packaging: packaging + '.asc')

    log("Deploying artifact group: ${artifact}")

    if (getDependency(pom, 'target/pom') && getDependency(artifact, 'target/artifact') && getDependency(signature, 'target/signature') && getDependency(pomSignature, 'target/pom-signature'))
    {
        assert deployFile(pomSignature, 'target/pom', 'target/pom-signature', repoUrl, repoId): "Failed to deploy [${pomSignature}]"
        assert deployFile(artifact, 'target/pom', 'target/artifact', repoUrl, repoId): "Failed to deploy [${artifact}]"
        assert deployFile(signature, 'target/pom', 'target/signature', repoUrl, repoId): "Failed to deploy [${signature}]"
    }
    else
    {
        log("Couldn't download dependency or pom [${groupId}:${artifactId}:${version}:${packaging}:${classifier}].")
    }

    ['javadoc', 'tests', 'sources', 'test-sources'].each { it ->
        optional = artifact.having(classifier: it)
        signature = optional.having(packaging: packaging + '.asc')
        if (getDependency(optional, "target/${it}") && getDependency(signature, "target/${it}-signature"))
        {
            assert deployFile(optional, 'target/pom', "target/${it}", repoUrl, repoId);
            assert deployFile(optional, 'target/pom', "target/${it}-signature", repoUrl, repoId);
        }
    }
}

private boolean getDependency(Artifact artifact, String destFilename)
{
    return mvn([GET_PLUGIN, "-Dartifact=${artifact}", "-Ddest=${destFilename}", "-Dtransitive=false"])
}

private boolean deployFile(Artifact artifact, String pomFile, String artifactFile, String repoUrl, String repoId)
{
    def args = ["deploy:deploy-file", "-DpomFile=${pomFile}", "-Dpackaging=${artifact.packaging}", "-Dfile=${artifactFile}", "-Durl=${repoUrl}", "-DrepositoryId=${repoId}"]
    if (artifact.classifier)
    {
        args.add("-Dclassifier=${artifact.classifier}")
    }
    result = mvn(args)
    log((result ? "Deployed: " : "Failed to deploy: ") + artifact)
    return result
}

protected void deployCeDistributions(String version)
{
    deployToRemote(ceRepoUrl, ceRepoId, "org.mule.distributions", "mule", version, "jar", "embedded")
    deployToRemote(ceRepoUrl, ceRepoId, "org.mule.distributions", "mule", version, "jar", "tests")
    deployToRemote(ceRepoUrl, ceRepoId, "org.mule.distributions", "mule-standalone", version, "tar.gz")
    deployToRemote(ceRepoUrl, ceRepoId, "org.mule.distributions", "mule-standalone", version, "zip")
    deployToRemote(ceRepoUrl, ceRepoId, "org.mule.distributions", "mule-standalone", version, "jar", "tests")
    deployToRemote(ceRepoUrl, ceRepoId, "org.mule.distributions", "mule-standalone-light", version, "tar.gz")
    deployToRemote(ceRepoUrl, ceRepoId, "org.mule.distributions", "mule-standalone-light", version, "zip")
    deployToRemote(ceRepoUrl, ceRepoId, "org.mule.distributions", "mule-standalone-light", version, "jar", "tests")
    deployToRemote(ceRepoUrl, ceRepoId, "org.mule.distributions", "mule-distributions", version, "jar", "tests")
}

def mvn(List mvnArgs)
{
    boolean errorFound = false;
    mvnArgs = mvnArgs*.replaceAll(' ', "\\\\ ") // escaping spaces: -Dkey=value\ with\ spaces
    Process proc = "mvn -B -Dmaven.repo.local=$m2repo ${mvnArgs.join(' ')}".execute()
    proc.in.eachLine {
        if (it.startsWith('[ERROR]'))
        {
            println it; errorFound = true
        }
    }
    proc.waitForOrKill(FIVE_MINUTES)
    return !errorFound
}

def log(String message)
{
    println message
}

@AutoClone
class Artifact
{

    String artifactId;
    String groupId;
    String version;
    String packaging;
    String classifier;

    public String toString()
    {
        return "${groupId}:${artifactId}:${version}:${packaging}" + (classifier ? ":" + classifier : "")
    }

    Artifact having(Map fields)// String classifier = null, String packaging = null)
    {
        Artifact clone = this.clone()
        fields.each { key, value -> clone."${key}" = value }
        return clone;
    }
}


/**
  This should do the following:
  - scan the directory tree for all schema
  - infer the deploy location of the schema
  - scan the directory tree for all configurations
  - check that the configurations are consistent
  - generate a list of the schema and their deploy location (disabled)
  - generate a set of commands that can be used to deploy the schema

  This script will generate a script to upload the schemas via curl.
  It uses davfs to locate the schema locations.  The generated script
  requires that you set the following environment variables:
  - CURLUSER = your davfs username
  - CURLPASSWD =  your davfs password

  It expects to be run in the buildtools/scripts directory (see "root"
  variable below).

  Example schema and their locations:
    transports/file/src/main/resources/META-INF/mule-file.xsd
     -> http://www.mulesoft.org/schema/mule/file/3.1/mule-file.xsd
    modules/cxf/src/main/resources/META-INF/mule-cxf.xsd
     -> http://www.mulesoft.org/schema/mule/cxf/3.1/mule-cxf.xsd
    core/src/main/resources/META-INF/mule.xsd
     -> http://www.mulesoft.org/schema/mule/core/3.1/mule.xsd

  This script and the generated script have only been verified to
  work on Linux.

  $Id$
*/

// Schema version
version = ''

// Provide the location where https://dav.codehaus.org/dist/mule is mounted on your file system
// as a parameter to the script.
davfs = ''

// schema are indexed by the xsd file names (eg mule-foo.xsd is "foo")
def schemaNames = []
def schemaSources = [:]
def schemaDestinations = [:]
def schemaDestinationPaths = [:]

// assume we are running in the buildtools/scripts directory
root = "../.."

// destination base
def base = "http://www.mulesoft.org/schema/mule/"

// the structure of xsd locations
def corexsd = /.*(\/|\\)spring-config(\/|\\).*(\/|\\)mule\.xsd/
def servicexsd = /.*(\/|\\)spring-config(\/|\\).*(\/|\\)mule-(.*)\.xsd/
def testxsd = /.*(\/|\\)tests(\/|\\)([^\/]+)(\/|\\).*(\/|\\)mule-test\.xsd/
def otherxsd = /.*(\/|\\)(transports|modules|patterns)(\/|\\)([^\/]+)(\/|\\).*(\/|\\)mule-(.*)\.xsd/

def parseArguments(def arguments)
{
    def cliBuilder = new CliBuilder()
    cliBuilder.d(longOpt: "davmountpoint", required: true, args: 1, "mount point of the DAV filesystem or directory where you want to save the schemas")
    cliBuilder.v(longOpt: "version", required: true, args: 1, "schemas version")
    cliBuilder.h(longOpt: "help", "show usage info")
    cliBuilder.r(longOpt: "root", required: true, args: 1, "start scanning at this root folder")

    options = cliBuilder.parse(arguments)
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

    version = options.v

    root = options.r
    if (options.d)
    {
        davfs = options.d
    }
}

def scanForSchemaAndInferDestinations = {
    println "# "
    println "# scanning for schema"
    println "# "
    for (f in new AntBuilder().fileScanner {
          fileset(dir: root) {
            include(name:"**/*.xsd")
            exclude(name:"**/src/test/**/*.xsd")
            exclude(name:"**/target/**/*.xsd")
          }
        })
    {
    if (f.absolutePath ==~ corexsd) {
      schemaNames << "core"
      schemaSources.put("core", f)
      schemaDestinations.put("core", "${base}core/${version}/mule.xsd")
      schemaDestinationPaths.put("core", "${base}core/${version}")
    } else if (f.absolutePath ==~ testxsd) {
      schemaNames << "test"
      schemaSources.put("test", f)
      schemaDestinations.put("test", "${base}test/${version}/mule-test.xsd")
      schemaDestinationPaths.put("test", "${base}test/${version}")
    } else if (f.absolutePath ==~ otherxsd) {
      match = (f.absolutePath =~ otherxsd)
      name = match[0][7]
      schemaNames << name
      schemaSources.put(name, f)
      schemaDestinations.put(name, "${base}${name}/${version}/mule-${name}.xsd")
      schemaDestinationPaths.put(name, "${base}${name}/${version}")
    } else if (f.absolutePath ==~ servicexsd) {
      match = (f.absolutePath =~ servicexsd)
      name = match[0][4]
      schemaNames << name
      schemaSources.put(name, f)
      schemaDestinations.put(name, "${base}${name}/${version}/mule-${name}.xsd")
      schemaDestinationPaths.put(name, "${base}${name}/${version}")
    } else {
      println "# WARNING: ignoring $f"
    }
  }
}

def checkSchema = {
  println "# "
  println "# checking schema"
  println "# "
  for (name in schemaNames) {
    file = schemaSources[name]
    parser = new XmlParser().parse(file)
    for (element in parser.value) {
      if (element.name.localPart == "import") {
        namespace = element.attributes["namespace"]
        if (namespace.contains("mulesoft") && ! namespace.contains("core") && ! element.attributes.keySet().contains("schemaLocation")) {
          println "# WARNING: missing schema location"
          println "# in " + file
          println "# for " + namespace
        }
      }
    }
  }
}

def scanAndCheckConfigs = {
    println "# "
    println "# checking configurations"
    println "# "
    for (f in new AntBuilder().fileScanner {
        fileset(dir: root) {
            include(name: "**/*.xml")
            exclude(name: "**/target/**/*.xml")
        }
    })
    {
        try {
            // println "# parsing $f"
            def parser = new XmlParser().parse(f)
            for (key in parser?.attributes().keySet()) {
                if (key instanceof groovy.xml.QName && key.localPart == "schemaLocation") {
                    tokens = new StringTokenizer(parser.attributes()[key])
                    while (tokens.hasMoreTokens()) {
                        id = tokens.nextToken()
                        uri = tokens.nextToken()
                        match = (id =~ /.*mulesoft\.org.*mule\/([^\/]+)\/.*/)
                        if (match.matches()) {
                            name = match[0][1]
                            // we should really scan for test schema too,
                            // but there's only one^H^H^H a few
                            if (name != "parsers-test" && ! name.startsWith("nest-example")) {
                                if (! schemaNames.contains(name)) {
                                    println "# WARNING: missing schema for $name - see file $f"
                                } else if (uri != schemaDestinations[name]) {
                                    println "# WARNING: URI for $name is ${schemaDestinations[name]} (not ${uri}) - see file $f"
                                }
                            }
                        }
                    }
                }
            }
        } catch (e) {
            println "# WARNING: error parsing $f: $e"
        }
    }
}

def listSchema = {
  for (name in schemaNames) {
    println "# "
    println "# ${name}:"
    println "#   ${schemaSources[name]}"
    println "#   ${schemaDestinations[name]}"
  }
}

// this is just a suggestion
def generateDeployCommand = {
  println "# "
  println "# generating deploy command"
  println "# "
  for (name in schemaNames) {
    source = schemaSources[name]
    pathUri = new URI(schemaDestinationPaths[name])
    schemaUri = new URI(schemaDestinations[name])
    println "mkdir -p ${davfs}${pathUri.path}"
    println "curl -T $source https://dav.codehaus.org/dist/mule${schemaUri.path} --user " + '$CURLUSER:$CURLPASSWD'
    println "echo curl result code: " + '$?'
    println "echo diff result"
    println "diff $source ${davfs}${schemaUri.path}"
    println "echo diff result code: " + '$?'
  }
}

parseArguments(args)
scanForSchemaAndInferDestinations()
checkSchema()
// scanAndCheckConfigs() // This method validate all xml raising too many errors, even when we can safely publish schemas, perhaps we could include this in an other script specific for validating xmls
//listSchema()
generateDeployCommand()


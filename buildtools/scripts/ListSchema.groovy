/**
  
  This should do the following:
  - scan the directory tree for all schema
  - infer the deploy location of the schema
  - scan the directory tree for all configurations
  - check that the configurations are consistent
  - generate a list of the schema and their deploy location (disabled)
  - generate a set of commands that can be used to deploy the schema

  The last part is going to depend on the deploy method, so the final
  user will need to edit the code (see generateDeployCommand below).

  It expects to be run in the buildtools/scripts directory (see "root"
  variable below).

  Example schema and their locations:
    transports/file/src/main/resources/META-INF/mule-file.xsd
     -> http://www.mulesource.org/schema/mule/file/2.0/mule-file.xsd
    modules/acegi/src/main/resources/META-INF/mule-acegi.xsd
     -> http://www.mulesource.org/schema/mule/acegi/2.0/mule-acegi.xsd
    core/src/main/resources/META-INF/mule.xsd
     -> http://www.mulesource.org/schema/mule/core/2.0/mule.xsd

  $Id$
*/

// Schema version
version = 2.0

// Provide the location where https://dav.codehaus.org/dist/mule is mounted on your file system
// as a parameter to the script.
davfs = "DAVMountPoint"
if (args.size() > 0)
{
    davfs = args[0]
}

// schema are indexed by the xsd file names (eg mule-foo.xsd is "foo")
schemaNames = []
schemaSources = [:]
schemaDestinations = [:]
schemaDestinationPaths = [:]

// assume we are running in the buildtools/scripts directory
root = "../.."

// destination base
base = "http://www.mulesource.org/schema/mule/"

// the structure of xsd locations
corexsd = /.*(\/|\\)core(\/|\\).*(\/|\\)mule.xsd/
otherxsd = /.*(\/|\\)(transports|modules)(\/|\\)([^\/]+)(\/|\\).*(\/|\\)mule-(.*)\.xsd/

checkCurrentDirectory = {
  if (! (new File("").getCanonicalFile().getName() == "scripts")) {
    println ""
    println "WARNING: run from in the scripts directory"
    println ""
    System.exit(1)
  }
}

scanForSchemaAndInferDestinations = {
  println ""
  println "scanning for schema"
  println ""
  for (f in new AntBuilder().fileScanner {
                  fileset(dir:root) {
                    include(name:"**/*.xsd")
                    exclude(name:"**/src/test/**/*.xsd")
                    exclude(name:"**/target/**/*.xsd")
                  }
                }) {
    if (f.absolutePath ==~ corexsd) {
      schemaNames << "core"
      schemaSources.put("core", f)
      schemaDestinations.put("core", "${base}core/${version}/mule.xsd")
      schemaDestinationPaths.put("core", "${base}core/${version}")
    } else if (f.absolutePath ==~ otherxsd) {
      match = (f.absolutePath =~ otherxsd)
      name = match[0][7]
      schemaNames << name
      schemaSources.put(name, f)
      schemaDestinations.put(name, "${base}${name}/${version}/mule-${name}.xsd")
      schemaDestinationPaths.put(name, "${base}${name}/${version}")
    } else {
      println "WARNING: ignoring $f"
    }
  }
}

checkSchema = {
  println ""
  println "checking schema"
  println ""
  for (name in schemaNames) {
    file = schemaSources[name]
    parser = new XmlParser().parse(file)
    for (element in parser.value) {
      if (element.name.localPart == "import") {
        namespace = element.attributes["namespace"]
        if (namespace.contains("mulesource") && ! namespace.contains("core") && ! element.attributes.keySet().contains("schemaLocation")) {
          println "WARNING: missing schema location"
          println "in " + file
          println "for " + namespace
        }
      }
    }
  }
}

scanAndCheckConfigs = {
  println ""
  println "checking configurations"
  println ""
  for (f in new AntBuilder().fileScanner {
                  fileset(dir:root) {
                    include(name:"**/*.xml")
                    exclude(name:"**/target/**/*.xml")
                  }
                }) {
    try {
      parser = new XmlParser().parse(f)
      for (key in parser?.attributes.keySet()) {
        if (key instanceof groovy.xml.QName
            && key.localPart == "schemaLocation") {
          tokens = new StringTokenizer(parser.attributes[key])
          while (tokens.hasMoreTokens()) {
            id = tokens.nextToken()
            uri = tokens.nextToken()
            match = (id =~ /.*mulesource\.org.*mule\/([^\/]+)\/.*/)
            if (match.matches()) {
              name = match[0][1]
              // we should really scan for test schema too, 
              // but there's only one...
              if (name != "parsers-test") {
                if (! schemaNames.contains(name)) {
                  println "WARNING: missing schema for $name - see file $f"
                } else if (uri != schemaDestinations[name]) {
                  println "WARNING: URI for $name is ${schemaDestinations[name]} (not ${uri}) - see file $f"
                }
              }
            }
          }
        }
      }
    } catch (e) {
      println "WARNING: error parsing $f: $e"
    }
  }
}

listSchema = {
  for (name in schemaNames) {
    println ""
    println "${name}:"
    println "  ${schemaSources[name]}"
    println "  ${schemaDestinations[name]}"
  }
}

// this is just a suggestion
generateDeployCommand = {
  println ""
  println "generating deploy command"
  println ""
  for (name in schemaNames) {
    source = schemaSources[name]
    pathUri = new URI(schemaDestinationPaths[name])
    schemaUri = new URI(schemaDestinations[name])
    println "mkdir -p ${davfs}${pathUri.path}"
    println "cp $source ${davfs}${schemaUri.path}"
  }
}

checkCurrentDirectory()
scanForSchemaAndInferDestinations()
checkSchema()
scanAndCheckConfigs()
//listSchema()
generateDeployCommand()


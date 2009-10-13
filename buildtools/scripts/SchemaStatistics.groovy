/**
  
  This should do the following:
  - scan the directory tree for all configuration files (*.xml)
  - produce statistics by package that include
  -- basic type (old <mule>, spring, new <mule>, other)
  -- for "new <mule>" type, additional statistics:
  --- whether inbound/outbound endpoints are used
  --- whether namespaces other than mule: and spring: are used
  --- whether other namespaces are used for anything other than endpoints
  - in addition, track the total number of namespaces present

  The output is not that beautiful, but filtering lines not beginning with
  "#" should give a CSV file (except for errors)

  $Id$
*/

import org.codehaus.groovy.ant.FileScanner

// assume we are running in the buildtools/scripts directory
def root = "../.."

class Package {
  String base  // core, transports, etc
  String dir   // full path
  List configs // all configuration files found
  int xmlTypeCountError
  int xmlTypeCountOther
  int xmlTypeCountOldMule
  int xmlTypeCountNewMule
  int xmlTypeCountSpring
  int endpointContextSpecific
  int endpointTransportSpecific

  void scan() {
    AntBuilder ant = new AntBuilder()
    FileScanner scanner = ant.fileScanner {
      fileset (dir: dir) {
        include(name: "**/*.xml")
        exclude(name: "**/pom.xml")
        exclude(name: "**/target/**")
        exclude(name: "**/test-data/out/**")
      }
    }
    scanner.each {file -> parse(file)}
  }

  void parse(File file) {
    // println "# parsing " + file.name
    try {
      Node doc = new XmlParser().parse(file)
      String name = doc.name().localPart
      // println "# schema name: $name"
      if (name.equals("beans")) {
        addSpringConfig(file, doc)
      } else if (name.equals("mule")) {
        addNewMuleConfig(file, doc)
      } else if (name.equals("mule-unsafe")) {
        addNewMuleConfig(file, doc)
      } else if (name.equals("mule-configuration")) {
        addOldMuleConfig(file, doc)
      } else if (name.equals("project")) {
        addOtherConfig(file, doc, true) // ant build file
      } else if (name.equals("Envelope")) {
        addOtherConfig(file, doc, true) // soap envelope
      } else if (name.equals("process-definition")) {
        addOtherConfig(file, doc, true) // loan broker process defn
      } else if (name.equals("ehcache")) {
        addOtherConfig(file, doc, true) // eh cache
      } else if (name.equals("ejb-jar")) {
        addOtherConfig(file, doc, true) // ejb jar
      } else if (name.equals("openejb-jar")) {
        addOtherConfig(file, doc, true) // ejb jar
      } else if (name.equals("jbpm-configuration")) {
        addOtherConfig(file, doc, true) // jbpm
      } else if (name.equals("web-app")) {
        addOtherConfig(file, doc, true) // web.xml
      } else if (name.equals("SiebelMessage")) {
        addOtherConfig(file, doc, true) // 3rd party
      } else if (name.equals("plexus")) {
        addOtherConfig(file, doc, true) // 3rd party
      } else {
        addOtherConfig(file, doc, false)
      }
    } catch (Exception e) {
      addErrorConfig(file, e)
    }
  }

  void addErrorConfig(File file, Exception e) {
    xmlTypeCountError++
    println "# *** failed to parse " + file.name
    println "# " + e.message
  }

  void addOtherConfig(File file, Node doc, boolean quiet) {
    xmlTypeCountOther++
    if (! quiet) {
      println "# failed to identify " + file.name + " starting with " + doc.name
    }
  }

  void addSpringConfig(File file, Node doc) {
    xmlTypeCountSpring++
  }

  void addNewMuleConfig(File file, Node doc) {
    xmlTypeCountNewMule++
    def elements = doc.depthFirst()
    def endpoints = elements.findAll{e -> e.name.localPart == "inbound-endpoint" || e.name.localPart == "outbound-endpoint"};
    if (endpoints.size() > 0) {
      endpointContextSpecific++
    }
    if (endpoints.findAll{e -> e.name.namespaceURI != "http://www.mulesoft.org/schema/mule/core"}) {
      endpointTransportSpecific++
    }
  }

  void addOldMuleConfig(File file, Node doc) {
    if (! file.name.contains("legacy")) {
      println "# *** old config: " + file.name
    }
    xmlTypeCountOldMule++
  }

  String toString() {
    return "# " + base + " " + dir
  }

  String types() {
    return "# error: " + xmlTypeCountError + "; other: " + xmlTypeCountOther + "; spring: " + xmlTypeCountSpring + "; old mule: " + xmlTypeCountOldMule + "; new mule: " + xmlTypeCountNewMule
  }

  String dropRoot(File dir) {
    if (dir.name == base) {
      return ""
    } else {
      File parent = new File(dir.parent)
      return dropRoot(parent) + "/" + dir.name
    }
  }

  String csv() {
    return "\"" + base + "\",\"" + dropRoot(new File(dir)) + "\"," + xmlTypeCountError + "," + xmlTypeCountOther + "," + xmlTypeCountSpring + "," + xmlTypeCountOldMule + "," + xmlTypeCountNewMule + "," + endpointContextSpecific + "," + endpointTransportSpecific
  }

}

def checkCurrentDirectory = {
  if (! (new File("").getCanonicalFile().getName() == "scripts")) {
    println ""
    println "WARNING: run from in the scripts directory"
    println ""
    System.exit(1)
  }
}

// root directories to search
def baseDirectories = ["core", "examples", "tests", "modules", "transports"]
//def baseDirectories = ["transports"]

// find directories with maven structure (pom.xml and (src or conf))
def findPackages = {
  packages = []
  for (base in baseDirectories) {
    extendPackagesFrom(root, base, packages)
  }
  return packages
}

def extendPackagesFrom(root, base, packages) {
  ant = new AntBuilder()
  scanner = ant.fileScanner {
    fileset (dir: root + "/" + base) {
      include(name: "**/pom.xml")
    }
  }
  scanner.each { file ->
    dir = file.parent
    if (checkForDirectory(dir, "src") || checkForDirectory(dir, "conf")) {
      packages.add(new Package(dir: dir, base: base))
    }
  }
  return packages
}

def checkForDirectory(parent, child) {
  file = new File(parent, child)
  return file.exists() && file.isDirectory()
}



// driver
checkCurrentDirectory()
def packages = findPackages()
println "group,package,error-type,other-type,spring-type,old-mule-type,new-mule-type,context-endpoint,transport-endpoint"
for (pkg in packages) {
  println "# ------------------"
  pkg.scan()
  println pkg
  println pkg.types()
  println pkg.csv()
}

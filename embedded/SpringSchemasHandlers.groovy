// splash
log.info '*' * 80
log.info('Aggregating spring.handlers and spring.schemas'.center(80))
log.info '*' * 80

def muleRoot = "$project.build.directory/../../../"

log.info("Mule project root: ${new File(muleRoot).canonicalPath}")

def buildDir = new File("$project.build.directory")
if (buildDir.exists() == false)
{
    buildDir.mkdirs()
}

def fileScanner = ant.fileScanner
{
    fileset(dir: muleRoot) 
    {
        include(name: "**/spring.schemas")
        exclude(name: "**/target/**")
    }
}

new File("$project.build.directory/spring.schemas").withWriter
{ writer ->

    fileScanner.each
    { file ->

        writer.writeLine(file.text)
    }
}

fileScanner = ant.fileScanner
{
    fileset(dir: muleRoot) 
    {
        include(name: "**/spring.handlers")
        exclude(name: "**/target/**")
    }
}

new File("$project.build.directory/spring.handlers").withWriter
{ writer ->

    fileScanner.each
    { file ->

        writer.writeLine(file.text)
    }
}

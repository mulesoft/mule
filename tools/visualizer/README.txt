
Mule Configuration Grapher
--------------------------

This is a cool little tool for generating  graphs for Mule configuration files.

It is intended to be run mainly as a Maven 2 plugin, but should also run from
the command line (although with less options, currently).


Building
--------

In the project directory (mule-contrib/tools/visualizer) run
> mvn install
(you need to have Maven2 installed).  The jar file will be generated in the
target directory (and also added to your local repository)


Plugin Configuration
--------------------

See this package's pom for a simple configuration (the "plugins" element).
For more details, see 
http://maven.apache.org/guides/plugin/guide-java-plugin-development.html

The following parameters can be configured (simple string values unless
indicated otherwise):

files        - a list of mule config files - required
exec         - the Graphvix executable; can also be specified via MULE_GRAPHVIZ
               system property
workingdir   - prefix directory, default "target"
outputdir    - destination directory for output
outputfile   - destination file for output
caption      - figure caption
mappings   
keepdotfiles - boolean
combinefiles - boolean
urls
config
templateprops

showall      - all show parameters are boolean; "showall" forces all to true
showconnectors
showmodels
showconfig
showagents
showtransformers


Plugin Use
----------

The plugin can be associated with a particular build phase (see link above) 
or triggered directly using
> mvn org.mule.tools:mule-tools-visualizer:graph


Command-Line Options
--------------------

-files      A comma-seperated list of Mule configuration files (required)

-outputdir  The directory to write the generated graphs to. Defaults to 
            the current directory (optional)

-exec       The executable file used for Graph generation. Defaults to 
            ./win32/dot.exe (optional)

-caption    Default caption for the generated graphs. Defaults to the 'id' 
            attribute in the config file (optional)

-?          Displays usage help


Command-Line Use
----------------

This generates the jar file in the target drectory
> mvn package 

This builds a classpath in the file cp.txt
> mvn dependency:build-classpath -Dmavenxtep.cpFile=cp.txt

> java -cp `cat cp.txt`:target/mule-tools-visualizer-1.0.jar \
    org.mule.tools.visualizer.MuleVisualizer \
    -files src/test/resources/echo-config.xml


Third Party Executables
-----------------------

This package uses graphviz.  A binary is included for Windows.  On Unix
machines install graphviz separately and add it to the standard PATH.  
It should then be used automatically.  Alternatively, see "exec" option.
http://www.graphviz.org/

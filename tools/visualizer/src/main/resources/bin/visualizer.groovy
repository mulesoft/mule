import org.mule.util.SystemUtils
import org.mule.tools.visualizer.MuleVisualizer
import org.apache.commons.cli.Options
import org.apache.commons.cli.BasicParser
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.HelpFormatter

// setup allowed cmd options
def o = new Options()
// true for hasArgs
o.addOption('files', true, 'A comma-separated list of config files to visualize')
o.addOption('exec', true, 'Full path to DOT executable')
o.addOption('outputdir', true, 'Visualizer output directory (can add to existing documentation)')
o.addOption('debug', false, 'Debug output')

CommandLine cmdLine =  new BasicParser().parse(o, args, true)
// automatically generate the help statement
if (cmdLine.getOptions().size() == 0) {
    HelpFormatter fmt = new HelpFormatter()
    fmt.printHelp( "visualizer -files <files> [-outputdir <dir>] [-exec <file>] [-debug]",
                   "", // header
                   o ,
                   "\nMore details about visualizer can be found here:\nhttp://www.muledocs.org/Config+Visualizer"  )
    System.exit(0)
}

// files is mandatory
if (!cmdLine.hasOption('files')) {
    println '\n\nMandatory parameter missing: files\n\n'
    System.exit(-1)
}
def files = cmdLine.getOptionValue('files')

// outputdir with fallback to the defaults
def outputdir = cmdLine.getOptionValue('outputdir','visualizer-output')

// dot executable
def exec = ''
if (cmdLine.hasOption('exec')) {
    exec = cmdLine.getOptionValue('exec')
} else {
    exec = '../lib/native/visualizer/dot'
    if (SystemUtils.IS_OS_WINDOWS) {
        // need a real file name for windows
        exec += '.exe'
    }

    // for unix flavor check local path and fallback to a globally installed package if none found
    if (SystemUtils.IS_OS_UNIX && !new File(exec).exists()) {
        def Process process = 'which dot'.execute()
        def out = process.text
        def err = process.err.text
        if (!out || err.contains('ommand not found')) {
            println '\n\nGraph Visualization package not found, please see http://www.graphviz.org\n\n'
            System.exit(-1)
        }
        exec = out.trim() // found in global path
    }
}

// construct new (possibly modified) args
def newArgs = []
newArgs << '-exec' << exec << '-files' << files << '-outputdir' << outputdir << '-debug' << cmdLine.hasOption('debug')

MuleVisualizer.main(newArgs as String[])

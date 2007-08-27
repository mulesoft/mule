/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

import org.mule.util.SystemUtils
import org.mule.tools.visualizer.MuleVisualizer
import org.apache.commons.cli.Options
import org.apache.commons.cli.BasicParser
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Option

// setup allowed cmd options
def o = new Options()
// true for hasArgs
o.addOption('files', true, 'A comma-separated list of Mule configuration files (required)')
o.addOption('exec', true, 'The executable file used for Graph generation')
o.addOption('outputdir', true, 'The directory to write the generated graphs to. Defaults to the ./visualizer-output')
o.addOption('outputfile', true, "The name of the file to output.\nDefault: Input filename + '.gif'")
o.addOption('caption', true, "Default caption for the generated graphs. Defaults to the 'id' attribute in the config file")
o.addOption('mappings', true, 'The path to a mappings file')
o.addOption('keepdotfiles', false, 'Keep temporary files')
o.addOption('combinefiles', false, 'Whether to include all files in a single diagram')
o.addOption('urls', true, 'File conaining url patterns for api docs')
o.addOption('config', true, 'Custom config for DOT executable')
o.addOption('workingdir', true, 'Working directory')
o.addOption('debug', false, 'Debug output')

o.addOption('showconnectors', true, 'Include connectors in the plot. Default: true')
o.addOption('showmodels', false, 'Include models in the plot')
o.addOption('showconfig', false, 'Include configs')
o.addOption('showagents', false, 'Include agents')
o.addOption('showtransformers', false, 'Include transformers')
o.addOption('showall', false, 'Forces all other show parameters to true')

CommandLine cmdLine =  new BasicParser().parse(o, args, true)
// automatically generate the help statement
if (cmdLine.getOptions().size() == 0) {
    HelpFormatter fmt = new HelpFormatter()
    fmt.printHelp( "visualizer -files <files> [-outputdir <dir>] [-exec <file>] [-outputfile <file>] [-caption <id>] " +
                   "[-mappings <file>] [-urls <file>] [-config <file>] [-workingdir <dir>] [-combinefiles] " +
                   "[-keepdotfiles] [-showconnectors <true|false>] [-showmodels] [-showconfig] [-showconfig] " +
                   "[-showagents] [-showtransformers] [-showall] [-debug]",
                   "\nMule Configuration Grapher\n" +  // header
                   "Generates graphs for Mule configuration files\n" +
                   "-----------------------------------------------",
                   o,
                   "\nMore details about visualizer can be found at:\nhttp://www.muledocs.org/Config+Visualizer"  )
    System.exit(0)
}

// files is mandatory
if (!cmdLine.hasOption('files')) {
    println '\n\nMandatory parameter missing: files\n\n'
    System.exit(-1)
}

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
        if (!out || err.contains('ommand not found')) { // miss out the first letter on purpose to be case-insensitive
            println '\n\nGraph Visualization package not found, please see http://www.graphviz.org\n\n'
            System.exit(-1)
        }
        exec = out.trim() // found in global path
    }
}

// construct new (possibly modified) args
def newArgs = []

newArgs << '-exec' << exec

// outputdir with fallback to the defaults
newArgs << '-outputdir' << cmdLine.getOptionValue('outputdir', 'visualizer-output')

cmdLine.options.each { Option option ->
    if (newArgs.contains("-${option.opt}")) {
        return
    }

    if (option.hasArg()) {
        newArgs << "-${option.opt}" << cmdLine.getOptionValue(option.opt)
    } else {
        newArgs << "-${option.opt}" << cmdLine.hasOption(option.opt)
    }
}

MuleVisualizer.main(newArgs as String[])

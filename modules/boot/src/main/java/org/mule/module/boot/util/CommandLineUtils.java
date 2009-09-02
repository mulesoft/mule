/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.boot.util;

import org.mule.api.DefaultMuleException;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CommandLineUtils
{

    private static CommandLine parseCommandLine(String args[], String opts[][]) throws DefaultMuleException
    {
        Options options = new Options();
        for (int i = 0; i < opts.length; i++)
        {
            options.addOption(opts[i][0], opts[i][1].equals("true") ? true : false, opts[i][2]);
        }

        BasicParser parser = new BasicParser();

        try
        {
            CommandLine line = parser.parse(options, args, true);
            if (line == null)
            {
                throw new DefaultMuleException("Unknown error parsing the Mule command line");
            }

            return line;
        }
        catch (ParseException p)
        {
            throw new DefaultMuleException("Unable to parse the Mule command line because of: "
                                           + p.toString(), p);
        }
    }

    /**
     * Returns the value corresponding to the given option from the command line, for
     * example if the options are "-config mule-config.xml"
     * getCommandLineOption("config") would return "mule-config.xml"
     */
    public static String getCommandLineOption(String option, String args[], String opts[][])
        throws DefaultMuleException
    {
        CommandLine line = parseCommandLine(args, opts);
        return line.getOptionValue(option);
    }

    /**
     * Checks whether a command line option is set. This is useful for command line
     * options that don't have an argument, like "-cluster", which means that this
     * Mule instance is part of a cluster.
     */
    public static boolean hasCommandLineOption(String option, String args[], String opts[][])
        throws DefaultMuleException
    {
        CommandLine line = parseCommandLine(args, opts);
        return line.hasOption(option);
    }

    /**
     * Returns a Map of all options in the command line. The Map is keyed off the
     * option name. The value will be whatever is present on the command line.
     * Options that don't have an argument will have the String "true".
     */
    public static Map getCommandLineOptions(String args[], String opts[][]) throws DefaultMuleException
    {
        CommandLine line = parseCommandLine(args, opts);
        Map ret = new HashMap();
        Option[] options = line.getOptions();

        for (int i = 0; i < options.length; i++)
        {
            Option option = options[i];
            ret.put(option.getOpt(), option.getValue("true"));
        }

        return ret;
    }
}

/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import org.mule.MuleException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

// @ThreadSafe
public class SystemUtils extends org.apache.commons.lang.SystemUtils
{
    protected static final Log logger = LogFactory.getLog(SystemUtils.class);

    // bash prepends: declare -x
    // zsh prepends: typeset -x
    private static final String[] UNIX_ENV_PREFIXES = new String[]{"declare -", "typeset -"};

    /**
     * Get the operating system environment variables. This should work for Windows
     * and Linux.
     * 
     * @return Map<String, String> or an empty map if there was an error.
     */
    public static synchronized Map getenv()
    {
        Map env = Collections./* <String, String> */EMPTY_MAP;

        try
        {
            if (SystemUtils.IS_JAVA_1_4)
            {
                // fallback to external process
                env = getenvJDK14();
            }
            else
            {
                // the following runaround is necessary since we still want to
                // compile on JDK 1.4
                Class target = System.class;
                Method envMethod = target.getMethod("getenv", ArrayUtils.EMPTY_CLASS_ARRAY);
                env = (Map) envMethod.invoke(target, (Class[] )null);
            }
        }
        catch (Exception ex)
        {
            // TODO MULE-863: Is this bad enough to fail?
            logger.error("Could not access OS environment: ", ex);
        }

        return env;
    }

    private static Map getenvJDK14() throws Exception
    {
        Map env = new HashMap();
        Process process = null;

        try
        {
            boolean isUnix = true;
            String command;

            if (SystemUtils.IS_OS_WINDOWS)
            {
                command = "cmd /c set";
                isUnix = false;
            }
            else
            {
                command = "env";
            }

            process = Runtime.getRuntime().exec(command);
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = br.readLine()) != null)
            {
                for (int prefix = 0; prefix < UNIX_ENV_PREFIXES.length; prefix++)
                {
                    if (line.startsWith(UNIX_ENV_PREFIXES[prefix]))
                    {
                        line = line.substring(UNIX_ENV_PREFIXES[prefix].length());
                    }
                }

                int index = -1;
                if ((index = line.indexOf('=')) > -1)
                {
                    String key = line.substring(0, index).trim();
                    String value = line.substring(index + 1).trim();
                    // remove quotes, if any
                    if (isUnix && value.length() > 1 && (value.startsWith("\"") || value.startsWith("'")))
                    {
                        value = value.substring(1, value.length() - 1);
                    }
                    env.put(key, value);
                }
                else
                {
                    env.put(line, StringUtils.EMPTY);
                }
            }
        }
        catch (Exception e)
        {
            throw e; // bubble up
        }
        finally
        {
            if (process != null)
            {
                process.destroy();
            }
        }

        return env;
    }

    public static String getenv(String name)
    {
        return (String) SystemUtils.getenv().get(name);
    }

    public static boolean isSunJDK()
    {
        return SystemUtils.JAVA_VM_VENDOR.toUpperCase().indexOf("SUN") != -1;
    }

    public static boolean isIbmJDK()
    {
        return SystemUtils.JAVA_VM_VENDOR.toUpperCase().indexOf("IBM") != -1;
    }

    /** 
     * @deprecated Command-line arguments will be handled exclusively by the bootloader in 2.0 
     */
    private static CommandLine parseCommandLine(String args[], String opts[][]) throws MuleException
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
                throw new MuleException("Unknown error parsing the Mule command line");
            }

            return line;
        }
        catch (ParseException p)
        {
            throw new MuleException("Unable to parse the Mule command line because of: " + p.toString(), p);
        }
    }

    /**
     * Returns the value corresponding to the given option from the command line, for
     * example if the options are "-config mule-config.xml"
     * getCommandLineOption("config") would return "mule-config.xml"
     * 
     * @deprecated Command-line arguments will be handled exclusively by the bootloader in 2.0
     */
    public static String getCommandLineOption(String option, String args[], String opts[][])
        throws MuleException
    {
        CommandLine line = parseCommandLine(args, opts);
        return line.getOptionValue(option);
    }

    /**
     * Checks whether a command line option is set. This is useful for command line
     * options that don't have an argument, like "-cluster", which means that this
     * Mule instance is part of a cluster.
     * 
     * @deprecated Command-line arguments will be handled exclusively by the bootloader in 2.0
     */
    public static boolean hasCommandLineOption(String option, String args[], String opts[][])
        throws MuleException
    {
        CommandLine line = parseCommandLine(args, opts);
        return line.hasOption(option);
    }

    /**
     * Returns a Map of all options in the command line. The Map is keyed off the
     * option name. The value will be whatever is present on the command line.
     * Options that don't have an argument will have the String "true".
     * 
     * @deprecated Command-line arguments will be handled exclusively by the bootloader in 2.0
     */
    public static Map getCommandLineOptions(String args[], String opts[][]) throws MuleException
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

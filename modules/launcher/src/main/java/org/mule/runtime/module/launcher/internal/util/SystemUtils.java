/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.internal.util;

import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// @ThreadSafe
public class SystemUtils extends org.mule.runtime.core.api.util.SystemUtils {

  // class logger
  protected static final Logger logger = LoggerFactory.getLogger(SystemUtils.class);

  private static CommandLine parseCommandLine(String args[], String opts[][]) throws MuleException {
    Options options = new Options();
    for (String[] opt : opts) {
      options.addOption(opt[0], opt[1].equals("true"), opt[2]);
    }

    BasicParser parser = new BasicParser();

    try {
      CommandLine line = parser.parse(options, args, true);
      if (line == null) {
        throw new DefaultMuleException("Unknown error parsing the Mule command line");
      }

      return line;
    } catch (ParseException p) {
      throw new DefaultMuleException("Unable to parse the Mule command line because of: " + p.toString(), p);
    }
  }

  /**
   * Returns a Map of all options in the command line. The Map is keyed off the option name. The value will be whatever is present
   * on the command line. Options that don't have an argument will have the String "true".
   */
  public static Map<String, Object> getCommandLineOptions(String args[], String opts[][]) throws MuleException {
    CommandLine line = parseCommandLine(args, opts);
    Map<String, Object> ret = new HashMap<>();
    Option[] options = line.getOptions();

    for (Option option : options) {
      ret.put(option.getOpt(), option.getValue("true"));
    }

    return ret;
  }

}

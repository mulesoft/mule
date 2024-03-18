/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.boot.commons.internal.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

// @ThreadSafe
public class SystemUtils {

  private static CommandLine parseCommandLine(String args[], String opts[][]) throws IllegalArgumentException {
    Options options = new Options();
    for (String[] opt : opts) {
      options.addOption(opt[0], opt[1].equals("true"), opt[2]);
    }

    BasicParser parser = new BasicParser();

    try {
      CommandLine line = parser.parse(options, args, true);
      if (line == null) {
        throw new IllegalArgumentException("Unknown error parsing the Mule command line");
      }

      return line;
    } catch (ParseException p) {
      throw new IllegalArgumentException("Unable to parse the Mule command line because of: " + p.toString(), p);
    }
  }

  /**
   * Returns a Map of all options in the command line. The Map is keyed off the option name. The value will be whatever is present
   * on the command line. Options that don't have an argument will have the String "true".
   */
  public static Map<String, Object> getCommandLineOptions(String args[], String opts[][]) throws IllegalArgumentException {
    CommandLine line = parseCommandLine(args, opts);
    Map<String, Object> ret = new HashMap<>();
    Option[] options = line.getOptions();

    for (Option option : options) {
      ret.put(option.getOpt(), option.getValue("true"));
    }

    return ret;
  }

}

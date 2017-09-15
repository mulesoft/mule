/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

import static org.apache.commons.lang3.StringUtils.INDEX_NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.indexOf;
import static org.apache.commons.lang3.StringUtils.substring;
import static org.apache.commons.lang3.SystemUtils.JAVA_VM_VENDOR;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_ENCODING_SYSTEM_PROPERTY;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.core.api.MuleContext;

import java.nio.charset.Charset;
import java.util.Collections;
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

public class SystemUtils {

  // class logger
  protected static final Logger logger = LoggerFactory.getLogger(SystemUtils.class);

  // the environment of the VM process
  private static Map environment = null;

  /**
   * Get the operating system environment variables. This should work for Windows and Linux.
   *
   * @return Map<String, String> or an empty map if there was an error.
   */
  public static synchronized Map getenv() {
    if (environment == null) {
      try {
        environment = System.getenv();
      } catch (Exception ex) {
        logger.error("Could not access OS environment: ", ex);
        environment = Collections.EMPTY_MAP;
      }
    }

    return environment;
  }

  public static String getenv(String name) {
    return (String) SystemUtils.getenv().get(name);
  }

  public static boolean isSunJDK() {
    return JAVA_VM_VENDOR.toUpperCase().indexOf("SUN") != -1
        || JAVA_VM_VENDOR.toUpperCase().indexOf("ORACLE") != -1;
  }

  public static boolean isAppleJDK() {
    return JAVA_VM_VENDOR.toUpperCase().indexOf("APPLE") != -1;
  }

  public static boolean isIbmJDK() {
    return JAVA_VM_VENDOR.toUpperCase().indexOf("IBM") != -1;
  }

  // TODO MULE-1947 Command-line arguments should be handled exclusively by the bootloader

  private static CommandLine parseCommandLine(String args[], String opts[][]) throws MuleException {
    Options options = new Options();
    for (String[] opt : opts) {
      options.addOption(opt[0], opt[1].equals("true") ? true : false, opt[2]);
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
  // TODO MULE-1947 Command-line arguments should be handled exclusively by the bootloader
  public static Map<String, Object> getCommandLineOptions(String args[], String opts[][]) throws MuleException {
    CommandLine line = parseCommandLine(args, opts);
    Map<String, Object> ret = new HashMap<String, Object>();
    Option[] options = line.getOptions();

    for (Option option : options) {
      ret.put(option.getOpt(), option.getValue("true"));
    }

    return ret;
  }

  /**
   * Returns a Map of all valid property definitions in <code>-Dkey=value</code> format. <code>-Dkey</code> is interpreted as
   * <code>-Dkey=true</code>, everything else is ignored. Whitespace in values is properly handled but needs to be quoted
   * properly: <code>-Dkey="some value"</code>.
   *
   * @param input String with property definitionn
   * @return a {@link Map} of property String keys with their defined values (Strings). If no valid key-value pairs can be parsed,
   *         the map is empty.
   */
  public static Map<String, String> parsePropertyDefinitions(String input) {
    if (StringUtils.isEmpty(input)) {
      return Collections.emptyMap();
    }

    // the result map of property key/value pairs
    final Map<String, String> result = new HashMap<String, String>();

    // where to begin looking for key/value tokens
    int tokenStart = 0;

    // this is the main loop that scans for all tokens
    findtoken: while (tokenStart < input.length()) {
      // find first definition or bail
      tokenStart = indexOf(input, "-D", tokenStart);
      if (tokenStart == INDEX_NOT_FOUND) {
        break findtoken;
      } else {
        // skip leading -D
        tokenStart += 2;
      }

      // find key
      int keyStart = tokenStart;
      int keyEnd = keyStart;

      if (keyStart == input.length()) {
        // short input: '-D' only
        break;
      }

      // let's check out what we have next
      char cursor = input.charAt(keyStart);

      // '-D xxx'
      if (cursor == ' ') {
        continue findtoken;
      }

      // '-D='
      if (cursor == '=') {
        // skip over garbage to next potential definition
        tokenStart = indexOf(input, ' ', tokenStart);
        if (tokenStart != INDEX_NOT_FOUND) {
          // '-D= ..' - continue with next token
          continue findtoken;
        } else {
          // '-D=' - get out of here
          break findtoken;
        }
      }

      // apparently there's a key, so find the end
      findkey: while (keyEnd < input.length()) {
        cursor = input.charAt(keyEnd);

        // '-Dkey ..'
        if (cursor == ' ') {
          tokenStart = keyEnd;
          break findkey;
        }

        // '-Dkey=..'
        if (cursor == '=') {
          break findkey;
        }

        // keep looking
        keyEnd++;
      }

      // yay, finally a key
      String key = substring(input, keyStart, keyEnd);

      // assume that there is no value following
      int valueStart = keyEnd;
      int valueEnd = keyEnd;

      // default value
      String value = "true";

      // now find the value, but only if the current cursor is not a space
      if (keyEnd < input.length() && cursor != ' ') {
        // bump value start/end
        valueStart = keyEnd + 1;
        valueEnd = valueStart;

        // '-Dkey="..'
        cursor = input.charAt(valueStart);
        if (cursor == '"') {
          // opening "
          valueEnd = indexOf(input, '"', ++valueStart);
        } else {
          // unquoted value
          valueEnd = indexOf(input, ' ', valueStart);
        }

        // no '"' or ' ' delimiter found - use the rest of the string
        if (valueEnd == INDEX_NOT_FOUND) {
          valueEnd = input.length();
        }

        // create value
        value = substring(input, valueStart, valueEnd);
      }

      // finally create key and value && loop again for next token
      result.put(key, value);

      // start next search at end of value
      tokenStart = valueEnd;
    }

    return result;
  }

  /**
   * @return the configured default encoding, checking in the following order until a value is found:
   *         <ul>
   *         <li>{@code muleContext} -> {@link org.mule.runtime.core.api.config.MuleConfiguration#getDefaultEncoding()}</li>
   *         <li>The value of the system property 'mule.encoding'</li>
   *         <li>{@code Charset.defaultCharset()}</li>
   *         </ul>
   */
  public static Charset getDefaultEncoding(MuleContext muleContext) {
    if (muleContext != null && muleContext.getConfiguration().getDefaultEncoding() != null) {
      return Charset.forName(muleContext.getConfiguration().getDefaultEncoding());
    } else if (System.getProperty(MULE_ENCODING_SYSTEM_PROPERTY) != null) {
      return Charset.forName(System.getProperty(MULE_ENCODING_SYSTEM_PROPERTY));
    } else {
      return Charset.defaultCharset();
    }
  }

}

/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

import static org.mule.runtime.core.api.config.MuleProperties.MULE_ENCODING_SYSTEM_PROPERTY;

import static org.apache.commons.lang3.StringUtils.INDEX_NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.indexOf;
import static org.apache.commons.lang3.StringUtils.substring;
import static org.apache.commons.lang3.SystemUtils.JAVA_VENDOR;
import static org.apache.commons.lang3.SystemUtils.JAVA_VM_NAME;
import static org.apache.commons.lang3.SystemUtils.JAVA_VM_VENDOR;

import org.mule.api.annotation.NoInstantiate;
import org.mule.runtime.api.config.ArtifactEncoding;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

// @ThreadSafe
/**
 * @deprecated make this class internal
 */
@Deprecated
@NoInstantiate
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
    return JAVA_VM_VENDOR.toLowerCase().contains("sun")
        || JAVA_VM_VENDOR.toLowerCase().contains("oracle");
  }

  public static boolean isAppleJDK() {
    return JAVA_VM_VENDOR.toLowerCase().contains("apple");
  }

  public static boolean isIbmJDK() {
    return JAVA_VM_VENDOR.toLowerCase().contains("ibm") || JAVA_VENDOR.toLowerCase().contains("ibm");
  }

  public static boolean isAzulJDK() {
    return JAVA_VM_VENDOR.toLowerCase().contains("azul");
  }

  public static boolean isAmazonJDK() {
    return JAVA_VM_VENDOR.toLowerCase().contains("amazon");
  }

  public static boolean isOpenJDK() {
    return JAVA_VM_VENDOR.toLowerCase().contains("openjdk") || JAVA_VENDOR.toLowerCase().contains("openjdk") ||
        JAVA_VM_NAME.toLowerCase().contains("openjdk");
  }

  public static boolean isAdoptOpenJDK() {
    return JAVA_VM_VENDOR.toLowerCase().contains("adoptopenjdk") || JAVA_VENDOR.toLowerCase().contains("adoptopenjdk");
  }

  public static boolean isAdoptiumTemurinJDK() {
    return JAVA_VM_VENDOR.toLowerCase().contains("temurin") || JAVA_VENDOR.toLowerCase().contains("temurin") ||
        JAVA_VM_VENDOR.toLowerCase().contains("adoptium") || JAVA_VENDOR.toLowerCase().contains("adoptium");
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
    final Map<String, String> result = new HashMap<>();

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
   * 
   * @deprecated since 4.10, {@link Inject @Inject} an {@link ArtifactEncoding} and call
   *             {@link ArtifactEncoding#getDefaultEncoding()} instead.
   */
  @Deprecated
  public static Charset getDefaultEncoding(MuleContext muleContext) {
    return getDefaultEncoding(muleContext != null ? muleContext.getConfiguration() : null);
  }

  /**
   * @return the configured default encoding, checking in the following order until a value is found:
   *         <ul>
   *         <li>{@code configuration} -> {@link org.mule.runtime.core.api.config.MuleConfiguration#getDefaultEncoding()}</li>
   *         <li>The value of the system property 'mule.encoding'</li>
   *         <li>{@code Charset.defaultCharset()}</li>
   *         </ul>
   * @deprecated since 4.10, {@link Inject @Inject} an {@link ArtifactEncoding} and call
   *             {@link ArtifactEncoding#getDefaultEncoding()} instead.
   */
  @Deprecated
  public static Charset getDefaultEncoding(MuleConfiguration configuration) {
    if (configuration != null && configuration.getDefaultEncoding() != null) {
      return Charset.forName(configuration.getDefaultEncoding());
    } else if (System.getProperty(MULE_ENCODING_SYSTEM_PROPERTY) != null) {
      return Charset.forName(System.getProperty(MULE_ENCODING_SYSTEM_PROPERTY));
    } else {
      return Charset.defaultCharset();
    }
  }

  protected SystemUtils() {}
}

/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * <code>PropertiesHelper</code> is a utility class for manipulating and filtering property Maps.
 * 
 * @deprecated Use {@link org.mule.runtime.core.util.api.PropertiesUtils} instead.
 */
// @ThreadSafe
@Deprecated
public final class PropertiesUtils {

  /** Do not instanciate. */
  private PropertiesUtils() {
    // no-op
  }

  public static void registerMaskedPropertyName(String name) {
    org.mule.runtime.core.util.api.PropertiesUtils.registerMaskedPropertyName(name);
  }

  /**
   * Returns the String representation of the property value or a masked String if the property key has been registered previously
   * via {@link #registerMaskedPropertyName(String)}.
   *
   * @param property a key/value pair
   * @return String of the property value or a "masked" String that hides the contents, or <code>null</code> if the property, its
   *         key or its value is <code>null</code>.
   */
  public static String maskedPropertyValue(Map.Entry<?, ?> property) {
    return org.mule.runtime.core.util.api.PropertiesUtils.maskedPropertyValue(property);
  }

  /**
   * Read in the properties from a properties file. The file may be on the file system or the classpath.
   *
   * @param fileName     - The name of the properties file
   * @param callingClass - The Class which is calling this method. This is used to determine the classpath.
   * @return a java.util.Properties object containing the properties.
   */
  public static synchronized Properties loadProperties(String fileName, final Class<?> callingClass) throws IOException {
    return org.mule.runtime.core.util.api.PropertiesUtils.loadProperties(fileName, callingClass);
  }

  public static Properties loadProperties(URL url) throws IOException {
    return org.mule.runtime.core.util.api.PropertiesUtils.loadProperties(url);
  }

  /**
   * Load all properties files in the classpath with the given properties file name.
   */
  public static Properties loadAllProperties(String fileName, ClassLoader classLoader) {
    return org.mule.runtime.core.util.api.PropertiesUtils.loadAllProperties(fileName, classLoader);
  }

  public static Properties loadProperties(InputStream is) throws IOException {
    return org.mule.runtime.core.util.api.PropertiesUtils.loadProperties(is);
  }

  public static String removeXmlNamespacePrefix(String eleName) {
    return org.mule.runtime.core.util.api.PropertiesUtils.removeXmlNamespacePrefix(eleName);
  }

  public static String removeNamespacePrefix(String eleName) {
    return org.mule.runtime.core.util.api.PropertiesUtils.removeNamespacePrefix(eleName);
  }

  public static Map removeNamespaces(Map properties) {
    return org.mule.runtime.core.util.api.PropertiesUtils.removeNamespaces(properties);
  }

  /**
   * Will create a map of properties where the names have a prefix Allows the callee to supply the target map so a comparator can
   * be set
   *
   * @param props    the source set of properties
   * @param prefix   the prefix to filter on
   * @param newProps return map containing the filtered list of properties or an empty map if no properties matched the prefix
   */
  public static void getPropertiesWithPrefix(Map props, String prefix, Map newProps) {
    org.mule.runtime.core.util.api.PropertiesUtils.getPropertiesWithPrefix(props, prefix, newProps);
  }

  public static Properties getPropertiesFromQueryString(String query) {
    return org.mule.runtime.core.util.api.PropertiesUtils.getPropertiesFromQueryString(query);
  }

  public static Properties getPropertiesFromString(String query, char separator) {
    return org.mule.runtime.core.util.api.PropertiesUtils.getPropertiesFromString(query, separator);
  }

  /**
   * Discovers properties files available on the classloader that loaded {@link PropertiesUtils} class
   *
   * @param resource resource to find. Not empty
   * @return a non null list of Properties
   * @throws IOException when a property file cannot be processed
   */
  public static List<Properties> discoverProperties(String resource) throws IOException {
    return org.mule.runtime.core.util.api.PropertiesUtils.discoverProperties(resource);
  }

  /**
   * Discovers properties files available on the given classloader.
   *
   * @param classLoader classloader used to find properties resources. Not null.
   * @param resource    resource to find. Not empty
   * @return a non null list of Properties
   * @throws IOException when a property file cannot be processed
   */
  public static List<Properties> discoverProperties(ClassLoader classLoader, String resource) throws IOException {
    return org.mule.runtime.core.util.api.PropertiesUtils.discoverProperties(classLoader, resource);
  }
}

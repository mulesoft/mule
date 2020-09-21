/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.util;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;

import static org.mule.runtime.dsl.internal.util.CollectionUtils.mergePropertiesIntoMap;
import static org.mule.runtime.dsl.internal.util.ResourceUtils.useCachesIfNecessary;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A helper class for loading mule schema mappings.
 *
 * @since 4.4.0
 */
public class SchemaMappingsLoaderUtils {

  private static final Logger LOGGER = getLogger(SchemaMappingsLoaderUtils.class);

  private SchemaMappingsLoaderUtils() {}

  /**
   * Load schemas mappings for a given {@code schemaMappingsLocation} location
   *
   * @param schemaMappingsLocation schema mappings location to load
   * @param classLoader {@link Supplier} the ClassLoader to use for loading schemas
   * @return a {@link Map} schemas mappings
   */
  public static Map<String, String> getSchemaMappings(String schemaMappingsLocation, Supplier<ClassLoader> classLoader) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Loading schema mappings from [" + schemaMappingsLocation + "]");
    }
    try {
      Properties appPluginsMappings = loadAllProperties(schemaMappingsLocation, classLoader);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Loaded schema mappings: " + appPluginsMappings);
      }
      Map<String, String> schemaMappings = new HashMap<>(appPluginsMappings.size());
      mergePropertiesIntoMap(appPluginsMappings, schemaMappings);
      return schemaMappings;
    } catch (IOException ex) {
      throw new IllegalStateException(
                                      "Unable to load schema mappings from location [" + schemaMappingsLocation + "]",
                                      ex);
    }
  }

  /**
   * Load all properties from the specified class path resource
   * (in ISO-8859-1 encoding), using the given class loader.
   * <p>Merges properties if more than one resource of the same name
   * found in the class path.
   * @param resourceName the name of the class path resource
   * @param classLoader {@link Supplier} the ClassLoader to use for loading (or {@code null} to use the default class loader)
   * @return the populated Properties instance
   * @throws IOException if loading failed
   */
  private static Properties loadAllProperties(String resourceName, Supplier<ClassLoader> classLoader) throws IOException {
    ClassLoader classLoaderToUse = classLoader.get();
    Enumeration<URL> urls =
        (classLoaderToUse != null ? classLoaderToUse.getResources(resourceName) : ClassLoader.getSystemResources(resourceName));
    Properties props = new Properties();
    while (urls.hasMoreElements()) {
      URL url = urls.nextElement();
      URLConnection con = url.openConnection();
      useCachesIfNecessary(con);
      InputStream is = con.getInputStream();
      try {
        if (resourceName != null && resourceName.endsWith(".xml")) {
          props.loadFromXML(is);
        } else {
          props.load(is);
        }
      } finally {
        is.close();
      }
    }
    return props;
  }

}

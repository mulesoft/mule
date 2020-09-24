/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.util;

import static org.springframework.util.CollectionUtils.mergePropertiesIntoMap;
import static org.springframework.util.ResourceUtils.useCachesIfNecessary;
import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A helper class for loading mule schema mappings.
 *
 * @since 4.4.0
 */
public class SchemaMappingsUtils {

  private static final String CORE_XSD = "http://www.mulesoft.org/schema/mule/core/current/mule.xsd";
  private static final String CORE_CURRENT_XSD = "http://www.mulesoft.org/schema/mule/core/current/mule-core.xsd";
  private static final String CORE_DEPRECATED_XSD = "http://www.mulesoft.org/schema/mule/core/current/mule-core-deprecated.xsd";
  private static final String COMPATIBILITY_XSD =
      "http://www.mulesoft.org/schema/mule/compatibility/current/mule-compatibility.xsd";

  private static final Logger LOGGER = getLogger(SchemaMappingsUtils.class);

  private SchemaMappingsUtils() {}

  /**
   * Override schema systemId according certain rule:
   * <p>
   * <ul>
   *     <li>Mule deprecated schema: Enable usage of 'mule-core-deprecated.xsd' for compatibility.
   *     If 'mule-core-deprecated.xsd' and 'mule-compatibility.xsd' can be resolved by {@code canResolveEntity},
   *     {@code systemId} will be replaced by 'mule-core-deprecated.xsd'. @see: MULE-13538 and MULE-13782</li>
   *
   *     <li>Legacy spring schemas: If {@code systemId} contains 'spring' keyword and ends with '-current.xsd',
   *     '-current.xsd' will be removed from {@code systemId}. @see: MULE-16572</li>
   * </ul>
   *
   * @param publicId The public identifier of the external entity being referenced, or null if none was supplied.
   * @param systemId The system identifier of the external entity being referenced.
   * @param canResolveEntity a {@link BiFunction} that return {@code true} if schema can be resolved for the given {@code publicId} and {@code systemId}
   * @return resolved systemId
   */
  public static String resolveSystemId(String publicId, String systemId,
                                       BiFunction<String, String, Boolean> canResolveEntity) {
    return resolveSystemId(publicId, systemId, false, canResolveEntity);
  }

  /**
   * Override schema {@code systemId} according certain rule:
   * <p>
   * <ul>
   *     <li>Enable usage of 'mule-core-deprecated.xsd' for testing purpose.
   *     If 'mule-core-deprecated.xsd' can be resolved by {@code canResolveEntity} and {@code runningTests} is {@code true},
   *     {@code systemId} will be replaced by 'mule-core-deprecated.xsd'. {@code systemId} @see: MULE-13538</li>
   *
   *     <li>Enable usage of 'mule-core-deprecated.xsd' for compatibility.
   *     If 'mule-core-deprecated.xsd' and 'mule-compatibility.xsd' can be resolved by {@code canResolveEntity},
   *     {@code systemId} will be replaced by 'mule-core-deprecated.xsd'. @see: MULE-13782</li>
   *
   *     <li>Legacy spring schemas. If {@code systemId} contains 'spring' keyword and ends with '-current.xsd',
   *     '-current.xsd' will be removed from {@code systemId}. @see: MULE-16572</li>
   * </ul>
   *
   * @param publicId The public identifier of the external entity being referenced, or null if none was supplied.
   * @param systemId The system identifier of the external entity being referenced.
   * @param runningTests true if running tests
   * @param canResolveEntity a {@link BiFunction} that return {@code true} if schema can be resolved for the given {@code publicId} and {@code systemId}
   * @return resolved systemId
   */
  public static String resolveSystemId(String publicId, String systemId, Boolean runningTests,
                                       BiFunction<String, String, Boolean> canResolveEntity) {
    if (systemId.equals(CORE_XSD)) {
      Boolean useDeprecated = canResolveEntity.apply(publicId, CORE_DEPRECATED_XSD);
      Boolean usingCompatibility = canResolveEntity.apply(publicId, COMPATIBILITY_XSD);

      if (useDeprecated && (usingCompatibility || runningTests)) {
        return CORE_DEPRECATED_XSD;
      } else {
        return CORE_CURRENT_XSD;
      }
    } else if (systemId.contains("spring")) {
      systemId = systemId.replace("-current.xsd", ".xsd");
    }

    return systemId;
  }

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

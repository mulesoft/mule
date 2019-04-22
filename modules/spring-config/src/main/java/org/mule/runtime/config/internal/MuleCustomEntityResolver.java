/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ResourceUtils;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

/**
 * Custom entity resolver based on Spring's schema resolver.
 *
 * @since 4.0
 */
public class MuleCustomEntityResolver implements EntityResolver {

  public static final String CUSTOM_SCHEMA_MAPPINGS_LOCATION = "META-INF/mule.schemas";
  public static final String CUSTOM_SPRING_SCHEMA_MAPPINGS_LOCATION = "META-INF/spring.schemas";
  private static final Logger LOGGER = LoggerFactory.getLogger(MuleCustomEntityResolver.class);

  private final ClassLoader classLoader;
  private final Map<String, String> muleSchemaMappings;
  private final Map<String, String> appPluginsSchemaMappings;

  MuleCustomEntityResolver(ClassLoader classLoader) {
    this.classLoader = classLoader;
    this.muleSchemaMappings = getMuleSchemaMappings();
    this.appPluginsSchemaMappings = getAppPluginsSchemaMappings();
  }

  @Override
  public InputSource resolveEntity(String publicId, String systemId) {
    if (systemId != null) {
      // Runtime takes precedence over plugins, to avoid a misbehaving plugin to override something from the runtime
      InputSource source =
          resoveEntityInClassloader(muleSchemaMappings, publicId, systemId, MuleCustomEntityResolver.class.getClassLoader());

      if (source == null) {
        source = resoveEntityInClassloader(appPluginsSchemaMappings, publicId, systemId, this.classLoader);
      }

      return source;
    }
    return null;
  }

  private static InputSource resoveEntityInClassloader(Map<String, String> schemaMappings, String publicId, String systemId,
                                                       ClassLoader cl) {
    String resourceLocation = schemaMappings.get(systemId);
    if (resourceLocation != null) {
      // The caller expects the stream in the InputSource to be open, so this cannot be closed before returning.
      InputStream is = cl.getResourceAsStream(resourceLocation);
      if (is == null) {
        LOGGER.debug("Couldn't find XML schema [" + systemId + "]: " + resourceLocation);
        return null;
      }
      InputSource source = new InputSource(is);
      source.setPublicId(publicId);
      source.setSystemId(systemId);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Found XML schema [" + systemId + "] in classpath: " + resourceLocation);
      }
      return source;
    } else {
      return null;
    }
  }

  /**
   * Load the specified schema mappings.
   */
  private Map<String, String> getMuleSchemaMappings() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Loading schema mappings from [" + CUSTOM_SCHEMA_MAPPINGS_LOCATION + "]");
    }
    try {
      Properties muleMappings =
          loadAllProperties(CUSTOM_SCHEMA_MAPPINGS_LOCATION, MuleCustomEntityResolver.class.getClassLoader());
      Properties springMappings =
          loadAllProperties(CUSTOM_SPRING_SCHEMA_MAPPINGS_LOCATION, MuleCustomEntityResolver.class.getClassLoader());
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Loaded Mule schema mappings: " + muleMappings);
        LOGGER.debug("Loaded Spring schema mappings: " + springMappings);
      }
      Map<String, String> schemaMappings = new HashMap<>(muleMappings.size(), springMappings.size());

      CollectionUtils.mergePropertiesIntoMap(muleMappings, schemaMappings);
      CollectionUtils.mergePropertiesIntoMap(springMappings, schemaMappings);
      return schemaMappings;
    } catch (IOException ex) {
      throw new IllegalStateException(
                                      "Unable to load schema mappings from location [" + CUSTOM_SCHEMA_MAPPINGS_LOCATION + "]",
                                      ex);
    }
  }

  /**
   * Load the specified schema mappings.
   */
  private Map<String, String> getAppPluginsSchemaMappings() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Loading schema mappings from [" + CUSTOM_SCHEMA_MAPPINGS_LOCATION + "]");
    }
    try {
      Properties appPluginsMappings =
          loadAllProperties(CUSTOM_SCHEMA_MAPPINGS_LOCATION, this.classLoader);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Loaded App Plugins schema mappings: " + appPluginsMappings);
      }
      Map<String, String> schemaMappings = new HashMap<>(appPluginsMappings.size());

      CollectionUtils.mergePropertiesIntoMap(appPluginsMappings, schemaMappings);
      return schemaMappings;
    } catch (IOException ex) {
      throw new IllegalStateException(
                                      "Unable to load schema mappings from location [" + CUSTOM_SCHEMA_MAPPINGS_LOCATION + "]",
                                      ex);
    }
  }

  /**
   * Load all properties from the specified class path resource
   * (in ISO-8859-1 encoding), using the given class loader.
   * <p>Merges properties if more than one resource of the same name
   * found in the class path.
   * @param resourceName the name of the class path resource
   * @param classLoader the ClassLoader to use for loading
   * (or {@code null} to use the default class loader)
   * @return the populated Properties instance
   * @throws IOException if loading failed
   */
  public static Properties loadAllProperties(String resourceName, ClassLoader classLoader) throws IOException {
    Assert.notNull(resourceName, "Resource name must not be null");
    ClassLoader classLoaderToUse = classLoader;
    if (classLoaderToUse == null) {
      classLoaderToUse = ClassUtils.getDefaultClassLoader();
    }
    Enumeration<URL> urls =
        (classLoaderToUse != null ? classLoaderToUse.getResources(resourceName) : ClassLoader.getSystemResources(resourceName));
    Properties props = new Properties();
    while (urls.hasMoreElements()) {
      URL url = urls.nextElement();
      URLConnection con = url.openConnection();
      ResourceUtils.useCachesIfNecessary(con);
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

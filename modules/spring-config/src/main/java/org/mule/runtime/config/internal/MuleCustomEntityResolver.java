/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static org.mule.runtime.config.internal.util.SchemaMappingsLoaderUtils.getSchemaMappings;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.InputStream;
import java.util.Map;

import org.slf4j.Logger;
import org.springframework.util.ClassUtils;
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
  private static final Logger LOGGER = getLogger(MuleCustomEntityResolver.class);

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
          resolveEntityInClassloader(muleSchemaMappings, publicId, systemId, MuleCustomEntityResolver.class.getClassLoader());

      if (source == null) {
        source = resolveEntityInClassloader(appPluginsSchemaMappings, publicId, systemId, this.classLoader);
      }

      return source;
    }
    return null;
  }

  private static InputSource resolveEntityInClassloader(Map<String, String> schemaMappings, String publicId, String systemId,
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
    Map<String, String> schemaMappings =
        getSchemaMappings(CUSTOM_SCHEMA_MAPPINGS_LOCATION,
                          () -> getClassLoaderToUse(MuleCustomEntityResolver.class.getClassLoader()));
    Map<String, String> springMappings =
        getSchemaMappings(CUSTOM_SPRING_SCHEMA_MAPPINGS_LOCATION,
                          () -> getClassLoaderToUse(MuleCustomEntityResolver.class.getClassLoader()));
    schemaMappings.putAll(springMappings);
    return schemaMappings;
  }

  private ClassLoader getClassLoaderToUse(ClassLoader classLoader) {
    ClassLoader classLoaderToUse = classLoader;
    if (classLoaderToUse == null) {
      classLoaderToUse = ClassUtils.getDefaultClassLoader();
    }

    return classLoaderToUse;
  }

  /**
   * Load the specified schema mappings.
   */
  private Map<String, String> getAppPluginsSchemaMappings() {
    return getSchemaMappings(CUSTOM_SCHEMA_MAPPINGS_LOCATION, () -> getClassLoaderToUse(this.classLoader));
  }

}

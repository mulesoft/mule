/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracing.level.impl.config;

import static org.mule.runtime.api.util.MuleSystemProperties.TRACING_LEVEL_CONFIGURATION_PATH;
import static org.mule.runtime.core.api.util.PropertiesUtils.loadProperties;

import static java.lang.String.format;
import static java.lang.System.getProperty;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.config.internal.model.dsl.ClassLoaderResourceProvider;
import org.mule.runtime.container.api.MuleFoldersUtil;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.tracing.level.api.config.TracingLevel;
import org.mule.runtime.tracing.level.api.config.TracingLevelConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;

import org.slf4j.Logger;

/**
 * A {@link TracingLevelConfiguration} based on a file in the conf folder.
 *
 * @since 4.5.0
 */
public class FileTracingLevelConfiguration implements TracingLevelConfiguration {

  private final String CONFIGURATION_PATH =
      getProperty(TRACING_LEVEL_CONFIGURATION_PATH, getConfFolder() + FileSystems.getDefault().getSeparator());

  private final MuleContext muleContext;

  private static final String CONFIGURATION_FILE_NAME = "tracing-level.conf";
  private static final String LEVEL_PROPERTY_NAME = "level";
  private static final TracingLevel DEFAULT_LEVEL = TracingLevel.MONITORING;
  private static final Logger LOGGER = getLogger(FileTracingLevelConfiguration.class);
  private final HashMap<String, TracingLevel> tracingLevelOverrides = new HashMap<>();
  private TracingLevel tracingLevel = DEFAULT_LEVEL;

  public FileTracingLevelConfiguration(MuleContext muleContext) {
    this.muleContext = muleContext;
    setTracingLevels();
  }

  private void setTracingLevels() {
    Properties properties = getTracingLevelProperties();
    setTracingLevel(properties);
    properties.remove(LEVEL_PROPERTY_NAME);
    setTracingLevelOverrides(properties);
  }

  private void setTracingLevel(Properties properties) {
    if (properties.containsKey(LEVEL_PROPERTY_NAME)) {
      try {
        tracingLevel = TracingLevel.valueOf(properties.getProperty(LEVEL_PROPERTY_NAME).toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
        LOGGER.info(format("Wrong tracing level found in configuration file. The tracing level will be set to the default: %s",
                           DEFAULT_LEVEL));
      }
    }
  }

  private void setTracingLevelOverrides(Properties properties) {
    properties.forEach((location, override) -> {
      try {
        tracingLevelOverrides.put(location.toString(), TracingLevel.valueOf(override.toString().toUpperCase(Locale.ROOT)));
      } catch (IllegalArgumentException e) {
        LOGGER.info("Wrong tracing level found in configuration file. This tracing level will be ignored.");
      }
    });
  }

  @Override
  public TracingLevel getTracingLevel(String location) {
    TracingLevel tracingLevelOverride = getTracingLevelOverride(location);
    if (tracingLevelOverride != null) {
      return tracingLevelOverride;
    }
    return tracingLevel;
  }

  private TracingLevel getTracingLevelOverride(String location) {
    return tracingLevelOverrides.get(location);
  }

  private Properties getTracingLevelProperties() {
    ClassLoaderResourceProvider resourceProvider = new ClassLoaderResourceProvider(getExecutionClassLoader(muleContext));
    try {
      InputStream is = resourceProvider
          .getResourceAsStream(CONFIGURATION_PATH + getPropertiesFileName());
      return loadProperties(is);
    } catch (MuleRuntimeException | IOException e) {
      LOGGER.info(format("No tracing level config found in the conf directory. The tracing level will be set to the default: %s",
                         DEFAULT_LEVEL));
    }
    return new Properties();
  }

  protected ClassLoader getExecutionClassLoader(MuleContext muleContext) {
    return muleContext.getExecutionClassLoader();
  }

  protected String getPropertiesFileName() {
    return CONFIGURATION_FILE_NAME;
  }

  protected String getConfFolder() {
    return MuleFoldersUtil.getConfFolder().getAbsolutePath();
  }

}

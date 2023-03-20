/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracing.level.impl.config;

import static org.mule.runtime.core.api.util.PropertiesUtils.loadProperties;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.config.internal.dsl.model.ClassLoaderResourceProvider;
import org.mule.runtime.container.api.MuleFoldersUtil;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.tracing.level.api.config.TracingLevel;
import org.mule.runtime.tracing.level.api.config.TracingLevelConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.util.Locale;
import java.util.Properties;

import org.slf4j.Logger;

/**
 * A {@link TracingLevelConfiguration} based on a file in the conf folder.
 *
 * @since 4.6.0
 */
public class FileTracingLevelConfiguration implements TracingLevelConfiguration {

  private final MuleContext muleContext;

  private static final String PROPERTIES_FILE_NAME = "tracing-level.conf";
  private static final String LEVEL_PROPERTY_NAME = "level";
  private static final TracingLevel DEFAULT_LEVEL = TracingLevel.MONITORING;
  private static final Logger LOGGER = getLogger(FileTracingLevelConfiguration.class);

  public FileTracingLevelConfiguration(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  @Override
  public TracingLevel getTracingLevel() {
    Properties properties = getTracingLevelProperties();
    if (properties != null && properties.getProperty(LEVEL_PROPERTY_NAME) != null) {
      try {
        return TracingLevel.valueOf(properties.getProperty(LEVEL_PROPERTY_NAME).toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
        LOGGER
            .info("Wrong tracing level found in configuration file. The tracing level will be set to the default: "
                + DEFAULT_LEVEL);
      }
    }
    return DEFAULT_LEVEL;
  }

  private Properties getTracingLevelProperties() {
    ClassLoaderResourceProvider resourceProvider = new ClassLoaderResourceProvider(getExecutionClassLoader(muleContext));
    try {
      InputStream is = resourceProvider
          .getResourceAsStream(getConfFolder() + FileSystems.getDefault().getSeparator() + getPropertiesFileName());
      return loadProperties(is);
    } catch (MuleRuntimeException | IOException e) {
      LOGGER
          .info("No tracing level config found in the conf directory. The tracing level will be set to the default: "
              + DEFAULT_LEVEL);
      e.printStackTrace();
    }
    return null;
  }

  protected ClassLoader getExecutionClassLoader(MuleContext muleContext) {
    return muleContext.getExecutionClassLoader();
  }

  protected String getPropertiesFileName() {
    return PROPERTIES_FILE_NAME;
  }

  protected String getConfFolder() {
    return MuleFoldersUtil.getConfFolder().getAbsolutePath();
  }

}

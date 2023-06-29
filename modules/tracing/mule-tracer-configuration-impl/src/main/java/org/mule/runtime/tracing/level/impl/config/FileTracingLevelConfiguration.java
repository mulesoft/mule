/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracing.level.impl.config;

import static org.mule.runtime.api.util.MuleSystemProperties.TRACING_LEVEL_CONFIGURATION_PATH;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.objectIsNull;

import static java.lang.String.format;
import static java.lang.System.getProperty;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.config.internal.model.dsl.ClassLoaderResourceProvider;
import org.mule.runtime.container.api.MuleFoldersUtil;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.tracing.level.api.config.TracingLevel;
import org.mule.runtime.tracing.level.api.config.TracingLevelConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
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
  private static final String LEVEL_PROPERTY_NAME = "mule.openTelemetry.tracer.level";
  private static final String OVERRIDES_PROPERTY_NAME = "mule.openTelemetry.tracer.levelOverrides";
  private static final TracingLevel DEFAULT_LEVEL = TracingLevel.MONITORING;
  private static final Logger LOGGER = getLogger(FileTracingLevelConfiguration.class);
  private static final ObjectMapper configFileMapper = new ObjectMapper(new YAMLFactory());
  private final HashMap<String, TracingLevel> tracingLevelOverrides = new HashMap<>();
  private TracingLevel tracingLevel = DEFAULT_LEVEL;
  private JsonNode configuration;

  public FileTracingLevelConfiguration(MuleContext muleContext) {
    this.muleContext = muleContext;
    setTracingLevels();
  }

  private void setTracingLevels() {
    configuration = getTracingLevelConfiguration();
    setTracingLevel();
    setTracingLevelOverrides();
  }

  private void setTracingLevel() {
    String configuredTracingLevel = readStringFromConfig(LEVEL_PROPERTY_NAME);
    if (configuredTracingLevel != null) {
      try {
        tracingLevel = TracingLevel.valueOf(configuredTracingLevel.toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
        LOGGER
            .error(format("Wrong tracing level found in configuration file: %s. The tracing level will be set to the default level: %s",
                          configuredTracingLevel, DEFAULT_LEVEL));
      }
    }
  }

  private void setTracingLevelOverrides() {
    readStringListFromConfig(OVERRIDES_PROPERTY_NAME).forEach((override) -> {
      String[] levelOverride = override.split("=");
      if (levelOverride.length != 2) {
        LOGGER.error(format("Wrong tracing level override found in configuration file: %s. This override will be ignored.",
                            override));
      } else {
        try {
          tracingLevelOverrides.put(levelOverride[0], TracingLevel.valueOf(levelOverride[1].toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
          LOGGER.error(format("Wrong tracing level override found in configuration file: %s. This override will be ignored.",
                              override));
        }
      }
    });
  }

  @Override
  public TracingLevel getTracingLevel() {
    return tracingLevel;
  }

  @Override
  public TracingLevel getTracingLevelOverride(String location) {
    TracingLevel tracingLevelOverride = getTracingLevelOverrideFrom(location);
    if (tracingLevelOverride != null) {
      return tracingLevelOverride;
    }
    return tracingLevel;
  }

  private TracingLevel getTracingLevelOverrideFrom(String location) {
    return tracingLevelOverrides.get(location);
  }

  private JsonNode getTracingLevelConfiguration() {
    ClassLoaderResourceProvider resourceProvider = new ClassLoaderResourceProvider(getExecutionClassLoader(muleContext));
    try {
      InputStream is = resourceProvider
          .getResourceAsStream(CONFIGURATION_PATH + getPropertiesFileName());
      return loadConfiguration(is);
    } catch (MuleRuntimeException | IOException e) {
      LOGGER
          .warn(format("Non existent or non parseable tracing level config file found. The tracing level will be set to the default: %s",
                       DEFAULT_LEVEL),
                e);
    }
    return null;
  }

  private static JsonNode loadConfiguration(InputStream is) throws IOException {
    if (is == null) {
      I18nMessage error = objectIsNull("input stream");
      throw new IOException(error.toString());
    }
    try {
      return configFileMapper.readTree(is);
    } finally {
      is.close();
    }
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

  private String readStringFromConfig(String key) {
    if (configuration != null) {
      String[] path = key.split("\\.");
      JsonNode configurationValue = configuration;
      for (int i = 0; i < path.length && configurationValue.get(path[i]) != null; i++) {
        configurationValue = configurationValue.get(path[i]);
      }
      return configurationValue != null ? configurationValue.textValue() : null;
    }
    return null;
  }

  private List<String> readStringListFromConfig(String key) {
    List<String> configuredValues = new ArrayList<>();
    if (configuration != null) {
      String[] path = key.split("\\.");
      JsonNode configurationValue = configuration;
      for (int i = 0; i < path.length && configurationValue != null; i++) {
        configurationValue = configurationValue.get(path[i]);
      }
      if (configurationValue != null) {
        configurationValue.elements().forEachRemaining(jsonNode -> {
          if (jsonNode.isTextual()) {
            configuredValues.add(jsonNode.textValue());
          }
        });
      }
    }
    return configuredValues;
  }

}

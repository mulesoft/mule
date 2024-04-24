/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracing.level.impl.config;

import static org.mule.runtime.api.util.MuleSystemProperties.TRACING_LEVEL_CONFIGURATION_PATH;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_TRACING_CONFIGURATION_FILE_PATH;
import static org.mule.runtime.tracing.level.api.config.TracingLevel.valueOf;

import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.nio.file.FileSystems.getDefault;
import static java.util.Collections.synchronizedList;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.config.api.properties.ConfigurationPropertiesResolver;
import org.mule.runtime.container.api.MuleFoldersUtil;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.observability.FileConfiguration;
import org.mule.runtime.tracing.level.api.config.TracingLevel;
import org.mule.runtime.tracing.level.api.config.TracingLevelConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;

/**
 * A {@link TracingLevelConfiguration} based on a file in the conf folder.
 *
 * @since 4.5.0
 */
public class FileTracingLevelConfiguration extends FileConfiguration implements TracingLevelConfiguration, Disposable {

  private static final String CONFIGURATION_FILE_NAME = "tracing-level.conf";
  private static final String LEVEL_PROPERTY_NAME = "mule.openTelemetry.tracer.level";
  private static final String OVERRIDES_PROPERTY_NAME = "mule.openTelemetry.tracer.levelOverrides";

  private static final Logger LOGGER = getLogger(FileTracingLevelConfiguration.class);

  private final HashMap<String, TracingLevel> tracingLevelOverrides = new HashMap<>();
  private final LazyValue<TracingLevel> tracingLevelInitializer = new LazyValue<>(() -> {
    initialise();
    return null;
  });
  private TracingLevel tracingLevel = null;

  private final MuleContext muleContext;
  private final YAMLConfiguration yamlConfiguration;

  public FileTracingLevelConfiguration(MuleContext muleContext) {
    super(muleContext);
    this.muleContext = muleContext;
    List<Runnable> onConfigurationChangeRunnables =
        synchronizedList(new ArrayList<>(Collections.singletonList(this::setTracingLevels)));
    String configurationFilePath = getProperty(MULE_OPEN_TELEMETRY_TRACING_CONFIGURATION_FILE_PATH,
                                               getConfFolder() + getDefault().getSeparator() + getPropertiesFileName());
    this.yamlConfiguration = new YAMLConfiguration(muleContext, onConfigurationChangeRunnables, configurationFilePath);
  }

  private void initialise() {
    setTracingLevels();
    yamlConfiguration.initialiseWatcher();
  }

  private void setTracingLevels() {
    yamlConfiguration.loadJSONConfiguration(getExecutionClassLoader(muleContext));
    setTracingLevel();
    setTracingLevelOverrides();
  }

  private void setTracingLevel() {
    String configuredTracingLevel = yamlConfiguration.getValue(LEVEL_PROPERTY_NAME);
    if (configuredTracingLevel != null) {
      try {
        tracingLevel = valueOf(configuredTracingLevel.toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
        LOGGER.error(format("Wrong tracing level found in configuration file: %s.", configuredTracingLevel));
        throw new MuleRuntimeException(e);
      }
    }
  }

  private void setTracingLevelOverrides() {
    List<String> overrides = yamlConfiguration.getStringListFromConfig(OVERRIDES_PROPERTY_NAME);
    overrides.forEach(override -> {
      String[] levelOverride = override.split("=");
      if (levelOverride.length != 2) {
        LOGGER.error(format("Wrong tracing level override found in configuration file: %s. This override will be ignored.",
                            override));
      } else {
        try {
          tracingLevelOverrides.put(levelOverride[0], valueOf(levelOverride[1].toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
          LOGGER.error(format("Wrong tracing level override found in configuration file: %s. This override will be ignored.",
                              override));
        }
      }
    });
  }

  @Override
  public TracingLevel getTracingLevel() {
    tracingLevelInitializer.get();
    return tracingLevel;
  }

  @Override
  public TracingLevel getTracingLevelOverride(String location) {
    tracingLevelInitializer.get();
    TracingLevel tracingLevelOverride = getTracingLevelOverrideFrom(location);
    return tracingLevelOverride != null ? tracingLevelOverride : tracingLevel;
  }

  @Override
  public void onConfigurationChange(Consumer<TracingLevelConfiguration> onConfigurationChangeConsumer) {
    yamlConfiguration.onConfigurationChange(() -> onConfigurationChangeConsumer.accept(this));
  }

  @Override
  public void dispose() {
    yamlConfiguration.dispose();
  }

  private TracingLevel getTracingLevelOverrideFrom(String location) {
    return tracingLevelOverrides.get(location);
  }

  protected ClassLoader getExecutionClassLoader(MuleContext muleContext) {
    return muleContext.getExecutionClassLoader();
  }

  protected String getPropertiesFileName() {
    return CONFIGURATION_FILE_NAME;
  }

  protected String getConfFolder() {
    return getProperty(TRACING_LEVEL_CONFIGURATION_PATH, MuleFoldersUtil.getConfFolder().getAbsolutePath());
  }

  @Override
  protected boolean isAValueCorrespondingToAPath(String key) {
    return yamlConfiguration.isAValueCorrespondingToAPath(key);
  }

  @Override
  protected JsonNode getConfiguration() {
    return yamlConfiguration.getConfiguration();
  }

  @Override
  protected ConfigurationPropertiesResolver getPropertyResolver() {
    return yamlConfiguration.getPropertyResolver();
  }
}

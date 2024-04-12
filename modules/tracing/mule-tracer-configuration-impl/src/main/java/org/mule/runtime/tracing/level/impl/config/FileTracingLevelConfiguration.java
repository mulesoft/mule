/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracing.level.impl.config;

import static org.mule.runtime.api.util.MuleSystemProperties.TRACING_LEVEL_CONFIGURATION_PATH;
import static org.mule.runtime.core.api.util.ClassUtils.getResourceOrFail;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_TRACING_CONFIGURATION_FILE_PATH;
import static org.mule.runtime.tracing.level.api.config.TracingLevel.valueOf;

import static org.slf4j.LoggerFactory.getLogger;

import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.util.Collections.synchronizedList;
import static java.util.Optional.empty;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.config.api.properties.ConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.model.dsl.ClassLoaderResourceProvider;
import org.mule.runtime.config.internal.model.dsl.config.DefaultConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.model.dsl.config.SystemPropertiesConfigurationProvider;
import org.mule.runtime.container.api.MuleFoldersUtil;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.observability.FileConfiguration;
import org.mule.runtime.tracer.common.watcher.TracingConfigurationFileWatcher;
import org.mule.runtime.tracing.level.api.config.TracingLevel;
import org.mule.runtime.tracing.level.api.config.TracingLevelConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileSystems;
import java.util.ArrayList;
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

  private final String CONFIGURATION_FILE_PATH =
      getProperty(MULE_OPEN_TELEMETRY_TRACING_CONFIGURATION_FILE_PATH,
                  getConfFolder() + FileSystems.getDefault().getSeparator() + getPropertiesFileName());

  private final MuleContext muleContext;

  private static final String CONFIGURATION_FILE_NAME = "tracing-level.conf";
  private static final String LEVEL_PROPERTY_NAME = "mule.openTelemetry.tracer.level";
  private static final String OVERRIDES_PROPERTY_NAME = "mule.openTelemetry.tracer.levelOverrides";
  private static final Logger LOGGER = getLogger(FileTracingLevelConfiguration.class);
  private final HashMap<String, TracingLevel> tracingLevelOverrides = new HashMap<>();
  private boolean tracingConfigurationFileWatcherInitialised;
  private TracingLevel tracingLevel = null;
  private JsonNode configuration;

  private List<Runnable> onConfigurationChangeRunnables = synchronizedList(new ArrayList<>());
  private URL configurationUrl;
  private TracingConfigurationFileWatcher tracingConfigurationFileWatcher;
  private ConfigurationPropertiesResolver propertyResolver;
  private boolean initialised;

  public FileTracingLevelConfiguration(MuleContext muleContext) {
    super(muleContext);
    this.muleContext = muleContext;
  }

  private Runnable getOnConfigurationChanged() {
    return () -> onConfigurationChangeRunnables.forEach(Runnable::run);
  }

  private void setTracingLevels() {
    configuration = getTracingLevelConfiguration();
    propertyResolver =
        new DefaultConfigurationPropertiesResolver(empty(),
                                                   new SystemPropertiesConfigurationProvider());
    setTracingLevel();
    setTracingLevelOverrides();
  }

  private void setTracingLevel() {
    String configuredTracingLevel = getStringValue(LEVEL_PROPERTY_NAME);
    if (configuredTracingLevel != null) {
      try {
        tracingLevel = valueOf(configuredTracingLevel.toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
        LOGGER.error(format("Wrong tracing level found in configuration file: %s.",
                            configuredTracingLevel));
        throw new MuleRuntimeException(e);
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
    // The levels are initialised here so that we can know if tracing is enabled and so that the logs are printed on the app's
    // logs
    if (tracingLevel == null) {
      initialise();
    }
    return tracingLevel;
  }

  private void initialise() {
    setTracingLevels();
    onConfigurationChangeRunnables.add(() -> setTracingLevels());
    if (configuration != null && !tracingConfigurationFileWatcherInitialised) {
      tracingConfigurationFileWatcher =
          new TracingConfigurationFileWatcher(configurationUrl.getFile(), getOnConfigurationChanged());
      tracingConfigurationFileWatcher.start();
      tracingConfigurationFileWatcherInitialised = true;
    }
    initialised = true;
  }

  @Override
  public TracingLevel getTracingLevelOverride(String location) {
    if (!initialised) {
      initialise();
    }
    TracingLevel tracingLevelOverride = getTracingLevelOverrideFrom(location);
    if (tracingLevelOverride != null) {
      return tracingLevelOverride;
    }
    return tracingLevel;
  }

  @Override
  public void onConfigurationChange(Consumer<TracingLevelConfiguration> onConfigurationChangeConsumer) {
    this.onConfigurationChangeRunnables.add(() -> onConfigurationChangeConsumer.accept(this));
  }

  @Override
  public void dispose() {
    if (tracingConfigurationFileWatcher != null) {
      tracingConfigurationFileWatcher.interrupt();
    }
  }

  private TracingLevel getTracingLevelOverrideFrom(String location) {
    return tracingLevelOverrides.get(location);
  }

  private JsonNode getTracingLevelConfiguration() {
    ClassLoaderResourceProvider resourceProvider = new ClassLoaderResourceProvider(getExecutionClassLoader(muleContext));
    try {
      InputStream is = resourceProvider
          .getResourceAsStream(CONFIGURATION_FILE_PATH);
      configurationUrl =
          getResourceOrFail(CONFIGURATION_FILE_PATH, getExecutionClassLoader(muleContext), true);
      return loadConfiguration(is);
    } catch (IOException e) {
      throw new MuleRuntimeException(e);
    }
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
    return false;
  }

  @Override
  protected JsonNode getConfiguration() {
    return configuration;
  }

  @Override
  protected ConfigurationPropertiesResolver getPropertyResolver() {
    return propertyResolver;
  }
}

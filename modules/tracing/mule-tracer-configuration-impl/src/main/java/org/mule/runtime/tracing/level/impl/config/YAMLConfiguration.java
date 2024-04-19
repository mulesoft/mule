/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracing.level.impl.config;

import static org.mule.runtime.core.api.util.ClassUtils.getResourceOrFail;

import static java.util.Optional.empty;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.config.api.properties.ConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.model.dsl.ClassLoaderResourceProvider;
import org.mule.runtime.config.internal.model.dsl.config.DefaultConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.model.dsl.config.SystemPropertiesConfigurationProvider;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.observability.FileConfiguration;
import org.mule.runtime.tracer.common.watcher.TracingConfigurationFileWatcher;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * YAML Configuration loads and manages the configuration.
 *
 * @since 4.8.0
 */
public class YAMLConfiguration extends FileConfiguration {

  private final ConfigurationPropertiesResolver propertyResolver;
  private final List<Runnable> onConfigurationChangeRunnables;
  private final String configurationFilePath;
  private JsonNode configuration;
  private URL configurationUrl;
  private TracingConfigurationFileWatcher tracingConfigurationFileWatcher;
  private boolean tracingConfigurationFileWatcherInitialised;

  public YAMLConfiguration(MuleContext muleContext, List<Runnable> onConfigurationChangeRunnables,
                           String configurationFilePath) {
    super(muleContext);
    this.onConfigurationChangeRunnables = onConfigurationChangeRunnables;
    this.configurationFilePath = configurationFilePath;
    this.propertyResolver = new DefaultConfigurationPropertiesResolver(empty(), new SystemPropertiesConfigurationProvider());
  }

  public void initialiseWatcher() {
    if (configuration != null && !tracingConfigurationFileWatcherInitialised) {
      tracingConfigurationFileWatcher =
          new TracingConfigurationFileWatcher(configurationUrl.getFile(),
                                              () -> onConfigurationChangeRunnables.forEach(Runnable::run));
      tracingConfigurationFileWatcher.start();
      tracingConfigurationFileWatcherInitialised = true;
    }
  }

  public void loadJSONConfigurationFromFile(ClassLoader classLoader) {
    ClassLoaderResourceProvider resourceProvider = new ClassLoaderResourceProvider(classLoader);
    try {
      InputStream is = resourceProvider.getResourceAsStream(configurationFilePath);
      configurationUrl = getResourceOrFail(configurationFilePath, classLoader, true);
      configuration = loadConfiguration(is);
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

  public String getValue(String property) {
    return getStringValue(property);
  }

  public List<String> getStringListFromConfig(String key) {
    return readStringListFromConfig(key);
  }

  public void onConfigurationChange(Runnable runnable) {
    this.onConfigurationChangeRunnables.add(runnable);
  }

  public void dispose() {
    if (tracingConfigurationFileWatcher != null) {
      tracingConfigurationFileWatcher.interrupt();
    }
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

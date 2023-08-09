/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.exporter.config.impl;

import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_TRACER_CONFIGURATION_AT_APPLICATION_LEVEL_PROPERTY;
import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.core.api.util.ClassUtils.getResourceOrFail;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsStream;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsUrl;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_CA_FILE_LOCATION;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_KEY_FILE_LOCATION;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_TRACING_CONFIGURATION_FILE_PATH;

import static java.lang.Boolean.getBoolean;
import static java.lang.System.getProperty;
import static java.util.Optional.empty;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.config.api.properties.ConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.model.dsl.ClassLoaderResourceProvider;
import org.mule.runtime.config.internal.model.dsl.config.DefaultConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.model.dsl.config.SystemPropertiesConfigurationProvider;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.observability.FileConfiguration;
import org.mule.runtime.tracer.common.watcher.TracingConfigurationFileWatcher;
import org.mule.runtime.tracer.exporter.config.api.SpanExporterConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A {@link SpanExporterConfiguration} based on a file in the conf folder.
 *
 * @since 4.5.0
 */
public class FileSpanExporterConfiguration extends FileConfiguration implements SpanExporterConfiguration, Disposable {

  private final MuleContext muleContext;

  public final static String MULE_TRACING_CONFIGURATION_PATH = SYSTEM_PROPERTY_PREFIX + "tracer.config.path";
  private final String PROPERTIES_FILE_NAME = getProperty(MULE_TRACING_CONFIGURATION_PATH, "tracer-exporter.conf");
  private ConfigurationPropertiesResolver propertyResolver;
  private JsonNode configuration;
  private ClassLoaderResourceProvider resourceProvider;
  private boolean propertiesInitialised;
  private URL configurationUrl;
  private final CompositeRunnable doOnConfigurationChanged = new CompositeRunnable();
  private TracingConfigurationFileWatcher tracingConfigurationFileWatcher;
  private String resolvedConfigurationFilePath;
  private boolean tracingConfigurationFileWatcherInitialised;

  public FileSpanExporterConfiguration(MuleContext muleContext) {
    super(muleContext);
    this.muleContext = muleContext;
  }

  @Override
  public String getStringValue(String key) {
    if (!propertiesInitialised) {
      initialiseProperties();
      propertiesInitialised = true;
    }

    return super.getStringValue(key);
  }

  protected boolean isAValueCorrespondingToAPath(String key) {
    return key.equals(MULE_OPEN_TELEMETRY_EXPORTER_CA_FILE_LOCATION) ||
        key.equals(MULE_OPEN_TELEMETRY_EXPORTER_KEY_FILE_LOCATION);
  }

  private void getSpanExporterConfigurationProperties() {
    if (getBoolean(ENABLE_TRACER_CONFIGURATION_AT_APPLICATION_LEVEL_PROPERTY)) {
      try {
        // This will verify first in the app and then in the conf folder.
        InputStream is = resourceProvider.getResourceAsStream(getPropertiesFileName());
        configuration = loadConfiguration(is);
        configurationUrl = getResourceOrFail(getPropertiesFileName(), getExecutionClassLoader(muleContext), true);
      } catch (MuleRuntimeException | IOException ignored) {
      }
    } else {
      try {
        String resourcePath = resolveConfigurationFilePath();
        InputStream is = getResourceAsStream(resourcePath, FileSpanExporterConfiguration.class);
        configuration = loadConfiguration(is);
        configurationUrl = getResourceAsUrl(resourcePath, FileSpanExporterConfiguration.class, true, true);
      } catch (IOException ignored) {
      }
    }
  }

  private String resolveConfigurationFilePath() {
    if (resolvedConfigurationFilePath != null) {
      return resolvedConfigurationFilePath;
    }
    resolvedConfigurationFilePath =
        getProperty(MULE_OPEN_TELEMETRY_TRACING_CONFIGURATION_FILE_PATH,
                    getConfFolder() + FileSystems.getDefault().getSeparator() + getPropertiesFileName());
    return resolvedConfigurationFilePath;
  }

  protected String getPropertiesFileName() {
    return PROPERTIES_FILE_NAME;
  }

  protected void initialiseProperties() {
    resourceProvider = new ClassLoaderResourceProvider(getExecutionClassLoader(muleContext));
    getSpanExporterConfigurationProperties();
    propertyResolver =
        new DefaultConfigurationPropertiesResolver(empty(),
                                                   new SystemPropertiesConfigurationProvider());
    if (configurationUrl != null && !tracingConfigurationFileWatcherInitialised) {
      tracingConfigurationFileWatcher = new TracingConfigurationFileWatcher(configurationUrl.getFile(), doOnConfigurationChanged);
      tracingConfigurationFileWatcher.start();
      tracingConfigurationFileWatcherInitialised = true;
    }
  }

  @Override
  public void doOnConfigurationChanged(Runnable doOnConfigurationChanged) {
    this.doOnConfigurationChanged.addRunnable(doOnConfigurationChanged);
  }

  @Override
  public void dispose() {
    if (tracingConfigurationFileWatcher != null) {
      tracingConfigurationFileWatcher.interrupt();
    }
  }

  @Override
  protected JsonNode getConfiguration() {
    return configuration;
  }

  @Override
  protected ConfigurationPropertiesResolver getPropertyResolver() {
    return propertyResolver;
  }

  private class CompositeRunnable implements Runnable {

    private final List<Runnable> runnables = new ArrayList<>();

    CompositeRunnable() {
      runnables.add(FileSpanExporterConfiguration.this::initialiseProperties);
    }

    @Override
    public void run() {
      runnables.forEach(Runnable::run);
    }

    public void addRunnable(Runnable runnable) {
      runnables.add(runnable);
    }
  }
}

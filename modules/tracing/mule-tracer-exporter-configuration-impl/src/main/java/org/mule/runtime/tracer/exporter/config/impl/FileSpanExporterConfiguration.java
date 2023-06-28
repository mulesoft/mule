/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.exporter.config.impl;

import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_TRACER_CONFIGURATION_AT_APPLICATION_LEVEL_PROPERTY;
import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.objectIsNull;
import static org.mule.runtime.core.api.util.ClassUtils.getResourceOrFail;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsStream;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsUrl;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_CA_FILE_LOCATION;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_KEY_FILE_LOCATION;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_TRACING_CONFIGURATION_FILE_PATH;

import static java.lang.Boolean.getBoolean;
import static java.lang.System.getProperty;
import static java.util.Optional.empty;

import static org.slf4j.LoggerFactory.getLogger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.config.api.properties.ConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.model.dsl.ClassLoaderResourceProvider;
import org.mule.runtime.config.internal.model.dsl.config.DefaultConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.model.dsl.config.SystemPropertiesConfigurationProvider;
import org.mule.runtime.container.api.MuleFoldersUtil;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.tracer.exporter.config.api.SpanExporterConfiguration;
import org.mule.runtime.tracer.exporter.config.impl.watcher.TracingConfigurationFileWatcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

/**
 * A {@link SpanExporterConfiguration} based on a file in the conf folder.
 *
 * @since 4.5.0
 */
public class FileSpanExporterConfiguration implements SpanExporterConfiguration, Disposable {

  private final MuleContext muleContext;

  public final static String MULE_TRACING_CONFIGURATION_PATH = SYSTEM_PROPERTY_PREFIX + "tracer.config.path";
  private final String PROPERTIES_FILE_NAME = getProperty(MULE_TRACING_CONFIGURATION_PATH, "tracer-exporter.conf");
  private static final Logger LOGGER = getLogger(FileSpanExporterConfiguration.class);
  private ConfigurationPropertiesResolver propertyResolver;
  private JsonNode configuration;
  private ClassLoaderResourceProvider resourceProvider;
  private boolean propertiesInitialised;
  private URL configurationUrl;
  private final CompositeRunnable doOnConfigurationChanged = new CompositeRunnable();
  private TracingConfigurationFileWatcher tracingConfigurationFileWatcher;
  private String resolvedConfigurationFilePath;
  private static final ObjectMapper configFileMapper = new ObjectMapper(new YAMLFactory());

  public FileSpanExporterConfiguration(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  @Override
  public String getStringValue(String key) {
    if (!propertiesInitialised) {
      initialiseProperties();
      propertiesInitialised = true;
    }

    String value = readStringFromConfigOrSystemProperty(key);

    if (value != null) {
      // Resolves the actual value when the current one is a system property reference.
      value = propertyResolver.apply(value);

      if (isAValueCorrespondingToAPath(key)) {
        // Obtain absolute path and return it if possible.
        String absolutePath = getAbsolutePath(value);
        if (absolutePath != null) {
          return absolutePath;
        }
        // Otherwise, return the non-resolved value so that the error message makes sense.
      }
      return value;
    }
    return null;
  }

  private String readStringFromConfigOrSystemProperty(String key) {
    if (configuration != null) {
      // We read the yaml configuration
      String[] path = key.split("\\.");
      JsonNode configurationValue = configuration;
      for (int i = 0; i < path.length && configurationValue.get(path[i]) != null; i++) {
        configurationValue = configurationValue.get(path[i]);
      }
      return configurationValue != null ? configurationValue.textValue() : null;
    } else {
      return getProperty(key);
    }
  }

  private String getAbsolutePath(String value) {
    Path path = Paths.get(value);

    try {
      if (!path.isAbsolute()) {
        URL url = getExecutionClassLoader(muleContext).getResource(value);

        if (url != null) {
          return new File(url.toURI()).getAbsolutePath();
        }
      }
    } catch (URISyntaxException e) {
      return value;
    }

    return null;
  }

  private boolean isAValueCorrespondingToAPath(String key) {
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
      } catch (MuleRuntimeException | IOException e) {
        LOGGER
            .info("No tracer exporter config found in the app or in the conf directory. Switching to system property based configuration.");
      }
    } else {
      try {
        String resourcePath = resolveConfigurationFilePath();
        InputStream is = getResourceAsStream(resourcePath, FileSpanExporterConfiguration.class);
        configuration = loadConfiguration(is);
        configurationUrl = getResourceAsUrl(resourcePath, FileSpanExporterConfiguration.class, true, true);
      } catch (IOException e) {
        LOGGER
            .info("No tracer exporter config found in the conf directory. Switching to system property based configuration.");
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

  protected String getConfFolder() {
    return MuleFoldersUtil.getConfFolder().getAbsolutePath();
  }

  protected String getPropertiesFileName() {
    return PROPERTIES_FILE_NAME;
  }

  public static JsonNode loadConfiguration(InputStream is) throws IOException {
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

  protected void initialiseProperties() {
    resourceProvider = new ClassLoaderResourceProvider(getExecutionClassLoader(muleContext));
    getSpanExporterConfigurationProperties();
    propertyResolver =
        new DefaultConfigurationPropertiesResolver(empty(),
                                                   new SystemPropertiesConfigurationProvider());
    if (configurationUrl != null) {
      tracingConfigurationFileWatcher = new TracingConfigurationFileWatcher(configurationUrl.getFile(), doOnConfigurationChanged);
      tracingConfigurationFileWatcher.start();
    }
  }

  protected ClassLoader getExecutionClassLoader(MuleContext muleContext) {
    return muleContext.getExecutionClassLoader();
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

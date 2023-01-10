/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.exporter.api.config;

import static org.mule.runtime.core.api.config.i18n.CoreMessages.objectIsNull;

import static java.lang.System.getProperties;
import static java.util.Optional.empty;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_CA_FILE_LOCATION;
import static org.mule.runtime.tracer.exporter.api.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_KEY_FILE_LOCATION;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.config.api.properties.ConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.dsl.model.ClassLoaderResourceProvider;
import org.mule.runtime.config.internal.dsl.model.config.DefaultConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.dsl.model.config.SystemPropertiesConfigurationProvider;
import org.mule.runtime.container.api.MuleFoldersUtil;
import org.mule.runtime.core.api.MuleContext;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * A {@link SpanExporterConfiguration} based on a file in the conf folder.
 *
 * @since 4.5.0
 */
public class FileSpanExporterConfiguration implements SpanExporterConfiguration, Initialisable {

  @Inject
  MuleContext muleContext;

  private static final String PROPERTIES_FILE_NAME = "tracer-exporter.conf";

  private static final Logger LOGGER = getLogger(FileSpanExporterConfiguration.class);

  private ConfigurationPropertiesResolver propertyResolver;
  private Properties properties;
  private ClassLoaderResourceProvider resourceProvider;

  @Override
  public String getStringValue(String key) {
    String value = properties.getProperty(key);

    if (value != null) {
      // We resolve to verify if it is a sysprop.
      value = propertyResolver.apply(value);

      if (isAValueCorrespondingToAPath(key)) {
        // We obtain the absolute path and return it if possible.
        String absolutePath = getAbsolutePath(value);

        if (absolutePath != null) {
          return absolutePath;
        }
      }

      return value;
    }

    return null;
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
    return key.equals(MULE_OPEN_TELEMETRY_EXPORTER_CA_FILE_LOCATION) || key.equals(
                                                                                   MULE_OPEN_TELEMETRY_EXPORTER_KEY_FILE_LOCATION);
  }

  private Properties getSpanExporterConfiguration() {
    try {
      // This will verify first in the app and then in the conf folder.
      InputStream is = resourceProvider.getResourceAsStream(getPropertiesFileName());
      return loadProperties(is);
    } catch (MuleRuntimeException | IOException e) {
      LOGGER.info("No tracer exporter config found in app. Loading it from the conf directory.");
      return getProperties();
    }
  }

  protected String getConfFolder() {
    return MuleFoldersUtil.getConfFolder().getAbsolutePath();
  }

  protected String getPropertiesFileName() {
    return PROPERTIES_FILE_NAME;
  }

  public static Properties loadProperties(InputStream is) throws IOException {
    if (is == null) {
      I18nMessage error = objectIsNull("input stream");
      throw new IOException(error.toString());
    }

    try {
      Properties props = new Properties();
      props.load(is);
      return props;
    } finally {
      is.close();
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    resourceProvider = new ClassLoaderResourceProvider(getExecutionClassLoader(muleContext));
    properties = getSpanExporterConfiguration();
    propertyResolver =
        new DefaultConfigurationPropertiesResolver(empty(),
                                                   new SystemPropertiesConfigurationProvider());
  }

  protected ClassLoader getExecutionClassLoader(MuleContext muleContext) {
    return muleContext.getExecutionClassLoader();
  }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.exporter.api.config;

import static java.lang.System.in;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.objectIsNull;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsStream;

import static java.lang.System.getProperties;
import static java.util.Optional.empty;
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
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
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

  public FileSpanExporterConfiguration() {}

  @Override
  public String getStringValue(String key) {
    String value = properties.getProperty(key);

    if (value != null) {
      return propertyResolver.apply(properties.getProperty(key));
    }

    if (key.equals(OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_CA_FILE_LOCATION)) {
      Path path = Paths.get(value);

      if (!path.isAbsolute()) {
        return muleContext.getExecutionClassLoader().getResource(resourceProvider.getResourceAsStream(path.toString())))git.getPath().
      }
    }

    return null;
  }

  private Properties getSpanExporterConfiguration() {
    try {
      InputStream is = resourceProvider.getResourceAsStream(getPropertiesFileName());
      return loadProperties(is);
    } catch (MuleRuntimeException | IOException e) {
      LOGGER.info("No tracer-exporter config found in app. Loading it from the conf.");
    }

    try {
      InputStream is = getResourceAsStream(getConfFolder() + FileSystems.getDefault().getSeparator() + getPropertiesFileName(),
                                           FileSpanExporterConfiguration.class);
      return loadProperties(is);
    } catch (IOException e) {
      LOGGER.info("No tracer-exporter config found in conf directory.");
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
    resourceProvider = new ClassLoaderResourceProvider(muleContext.getExecutionClassLoader());
    properties = getSpanExporterConfiguration();
    propertyResolver =
        new DefaultConfigurationPropertiesResolver(empty(),
                                                   new SystemPropertiesConfigurationProvider());
  }
}

/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.observability;

import static org.mule.runtime.core.api.config.i18n.CoreMessages.objectIsNull;

import static java.lang.System.getProperty;

import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.config.api.properties.ConfigurationPropertiesResolver;
import org.mule.runtime.container.api.MuleFoldersUtil;
import org.mule.runtime.core.api.MuleContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * A class that provides functionality to read configuration from a file.
 *
 * @since 4.6.0
 */
public abstract class FileConfiguration {

  private final MuleContext muleContext;
  private static final ObjectMapper configFileMapper = new ObjectMapper(new YAMLFactory());

  public FileConfiguration(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  public String getStringValue(String key) {
    String value = readStringFromConfigOrSystemProperty(key);

    if (value != null) {
      // Resolves the actual value when the current one is a system property reference.
      value = getPropertyResolver().apply(value);

      if (isAValueCorrespondingToAPath(key)) {
        // Obtain absolute path and return it if possible.
        String absolutePath = getAbsolutePath(value);

        if (absolutePath != null) {
          return absolutePath;
        }
        // Otherwise, return the non-resolved value so that the error message makes sense.
      }
    }
    return value;
  }

  private String readStringFromConfigOrSystemProperty(String key) {
    JsonNode configuration = getConfiguration();
    if (configuration != null) {
      // We read the yaml configuration
      String[] path = key.split("\\.");
      JsonNode configurationValue = configuration;
      for (int i = 0; i < path.length && configurationValue.get(path[i]) != null; i++) {
        configurationValue = configurationValue.get(path[i]);
      }
      return configurationValue != null && !configurationValue.asText().isEmpty() ? configurationValue.asText() : null;
    } else {
      return getProperty(key);
    }
  }

  protected abstract boolean isAValueCorrespondingToAPath(String key);

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

  protected ClassLoader getExecutionClassLoader(MuleContext muleContext) {
    return muleContext.getExecutionClassLoader();
  }

  protected static JsonNode loadConfiguration(InputStream is) throws IOException {
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

  protected String getConfFolder() {
    return MuleFoldersUtil.getConfFolder().getAbsolutePath();
  }

  protected abstract JsonNode getConfiguration();

  protected abstract ConfigurationPropertiesResolver getPropertyResolver();
}

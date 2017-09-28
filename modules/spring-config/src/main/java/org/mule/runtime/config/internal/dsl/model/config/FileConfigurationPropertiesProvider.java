/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.config;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.config.api.dsl.model.ResourceProvider;
import org.mule.runtime.core.api.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * File based properties provider, using an external resource provider.
 */
public class FileConfigurationPropertiesProvider implements ConfigurationPropertiesProvider {

  private final static String FILE_PREFIX = "file::";
  private ResourceProvider resourceProvider;
  private String description;

  public FileConfigurationPropertiesProvider(ResourceProvider resourceProvider, String description) {
    this.resourceProvider = resourceProvider;
    this.description = description;
  }

  @Override
  public Optional<ConfigurationProperty> getConfigurationProperty(String configurationAttributeKey) {
    if (configurationAttributeKey.startsWith(FILE_PREFIX)) {
      String path = configurationAttributeKey.substring(FILE_PREFIX.length());
      try (InputStream is = resourceProvider.getResourceAsStream(path)) {
        if (is != null) {
          String value = IOUtils.toString(is);
          return of(new ConfigurationProperty(this, configurationAttributeKey, value));
        }
      } catch (IOException e) {
        // ignore close exception
      }
    }

    return empty();
  }

  @Override
  public String getDescription() {
    return description;
  }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.crafted.config.properties.extension;

import static java.lang.String.format;
import static java.util.Optional.empty;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.config.api.dsl.model.ResourceProvider;
import org.mule.runtime.config.api.dsl.model.properties.ConfigurationProperty;
import org.mule.runtime.config.api.dsl.model.properties.DefaultConfigurationPropertiesProvider;

import java.util.Optional;

/**
 * Artifact attributes configuration. This class represents a single secure-configuration-properties element from the
 * configuration.
 *
 * @since 4.1
 */
public class SecureConfigurationPropertiesProvider extends DefaultConfigurationPropertiesProvider {

  private final static String SECURE_PREFIX = "secure::";
  private final String algorithm;
  private final String mode;

  public SecureConfigurationPropertiesProvider(ResourceProvider resourceProvider, String file, String algorithm, String mode) {
    super(file, resourceProvider);

    this.algorithm = algorithm;
    this.mode = mode;
  }

  @Override
  public Optional<ConfigurationProperty> getConfigurationProperty(String configurationAttributeKey) {
    if (configurationAttributeKey.startsWith(SECURE_PREFIX)) {
      String effectiveKey = configurationAttributeKey.substring(SECURE_PREFIX.length());
      return Optional.ofNullable(configurationAttributes.get(effectiveKey));
    } else {
      return empty();
    }
  }

  @Override
  public String getDescription() {
    ComponentLocation location = (ComponentLocation) getAnnotation(LOCATION_KEY);
    return format("<secure-configuration-properties file=\"%s\"> - file: %s, line number: %s", fileLocation,
                  location.getFileName().orElse(UNKNOWN),
                  location.getLineInFile().map(String::valueOf).orElse("unknown"));
  }

  @Override
  protected String createValue(String key, String value) {
    return format("%s:%s:%s:%s", key, value, algorithm, mode);
  }
}

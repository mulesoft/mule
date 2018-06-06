/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.crafted.config.properties.extension;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.config.api.dsl.model.ResourceProvider;
import org.mule.runtime.config.api.dsl.model.properties.ConfigurationProperty;
import org.mule.runtime.config.api.dsl.model.properties.DefaultConfigurationPropertiesProvider;
import org.mule.runtime.config.internal.dsl.model.config.DefaultConfigurationProperty;

import java.util.Optional;

/**
 * Artifact attributes configuration. This class represents a single secure-configuration-properties element from the
 * configuration.
 *
 * @since 4.1
 */
public class SecureConfigurationPropertiesProvider extends DefaultConfigurationPropertiesProvider
    implements Initialisable, Disposable {

  private final static String SECURE_PREFIX = "secure::";
  private final static String LIFECYCLE_PREFIX = "lifecycle::";
  private final String algorithm;
  private final String mode;
  private int initializationCount = 0;
  private int disposeCount = 0;

  public SecureConfigurationPropertiesProvider(ResourceProvider resourceProvider, String file, String algorithm, String mode) {
    super(file, resourceProvider);

    this.algorithm = algorithm;
    this.mode = mode;
  }

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    initializationCount++;
  }

  @Override
  public void dispose() {
    disposeCount++;
  }

  @Override
  public Optional<ConfigurationProperty> getConfigurationProperty(String configurationAttributeKey) {
    if (configurationAttributeKey.startsWith(SECURE_PREFIX)) {
      String effectiveKey = configurationAttributeKey.substring(SECURE_PREFIX.length());
      return Optional.ofNullable(configurationAttributes.get(effectiveKey));
    } else if (configurationAttributeKey.startsWith(LIFECYCLE_PREFIX)) {
      String effectiveKey = configurationAttributeKey.substring(LIFECYCLE_PREFIX.length());
      if ("initialize".equals(effectiveKey) || "dispose".equals(effectiveKey)) {
        return of(new ConfigurationProperty() {

          @Override
          public Object getSource() {
            return this;
          }

          @Override
          public Object getRawValue() {
            return "initialize".equals(effectiveKey) ? Integer.toString(initializationCount) : Integer.toString(disposeCount);
          }

          @Override
          public String getKey() {
            return effectiveKey;
          }
        });
      }
      return empty();
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

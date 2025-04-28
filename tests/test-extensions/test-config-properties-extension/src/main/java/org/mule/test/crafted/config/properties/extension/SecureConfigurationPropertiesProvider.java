/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.crafted.config.properties.extension;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.properties.api.ConfigurationProperty;
import org.mule.runtime.properties.api.DefaultConfigurationPropertiesProvider;
import org.mule.runtime.properties.api.ResourceProvider;

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

  /**
   * Returns the configuration property loaded from the file with the given key.
   * <p/>
   * MULE-15032: There are 2 special keys used for testing purposes: "lifecycle::initialize" and "lifecycle::dispose". If this
   * custom configuration properties provider is loaded by SPI in any integration test, asking for a property with any of those
   * keys will return the number of times this provider was initialized or disposed. This was added as a workaround for testing
   * that those phases were actually being applied to the ConfigurationPropertiesProviders when creating or clearing an
   * ApplicationModel. keep in mind that this extension is only intended for testing purposes and this behaviour will not be
   * replicated for any productive code.
   *
   * @param configurationAttributeKey
   * @return an Optional with the value of the given key or {@link Optional#empty()} otherwise.
   */
  @Override
  public Optional<ConfigurationProperty> provide(String configurationAttributeKey) {
    if (configurationAttributeKey.startsWith(SECURE_PREFIX)) {
      String effectiveKey = configurationAttributeKey.substring(SECURE_PREFIX.length());
      return super.provide(effectiveKey);
    } else if (configurationAttributeKey.startsWith(LIFECYCLE_PREFIX)) {
      String effectiveKey = configurationAttributeKey.substring(LIFECYCLE_PREFIX.length());
      if ("initialize".equals(effectiveKey) || "dispose".equals(effectiveKey)) {
        return of(new ConfigurationProperty() {

          @Override
          public Object getSource() {
            return this;
          }

          @Override
          public String getValue() {
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
  protected String createValue(String key, String value) {
    return format("%s:%s:%s:%s", key, value, algorithm, mode);
  }
}

/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.splash;

/**
 * Class to check that system properties are enabled or not, the latter being the default.
 *
 * @since 3.6.0
 */
public class PropertyChecker {

  private final String propertyName;
  private final String defaultValue;
  private Boolean override;

  /**
   * Creates a {@link PropertyChecker} with a custom default value.
   *
   * @param propertyName the name of the system property
   * @param defaultValue the default value to use in case the property is not set
   */
  public PropertyChecker(String propertyName, String defaultValue) {
    this.propertyName = propertyName;
    this.defaultValue = defaultValue;
  }

  /**
   * WARNING: This method looks up a system property, which can lead to contention if many threads access the system properties at
   * the same time. It is recommended to read this value during initialization and keeping the result, rather than calling this
   * method repeatedly.
   *
   * @return the value of the property, or the default if not present
   */
  public boolean isEnabled() {
    if (override == null) {
      return Boolean.valueOf(System.getProperty(propertyName, defaultValue));
    } else {
      return override;
    }
  }

  public void setOverride(Boolean override) {
    this.override = override;
  }

  public String getPropertyName() {
    return propertyName;
  }
}

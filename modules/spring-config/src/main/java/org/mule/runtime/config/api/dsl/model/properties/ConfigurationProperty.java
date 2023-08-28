/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.api.dsl.model.properties;

/**
 * Represents a configuration attribute.
 *
 * @since 4.1
 *
 * @deprecated since 4.4, use org.mule.runtime.properties.api.ConfigurationProperty instead.
 */
@Deprecated
public interface ConfigurationProperty extends org.mule.runtime.properties.api.ConfigurationProperty {

  /**
   * @return the plain configuration value without resolution. A configuration value may contain reference to other configuration
   *         attributes.
   */
  Object getRawValue();

  @Override
  default String getValue() {
    return getRawValue().toString();
  }
}

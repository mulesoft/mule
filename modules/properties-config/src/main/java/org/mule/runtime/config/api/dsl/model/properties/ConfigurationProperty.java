/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

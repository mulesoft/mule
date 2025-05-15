/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.properties;

import org.mule.runtime.config.api.dsl.model.properties.ConfigurationProperty;

/**
 * Represents a configuration attribute.
 * <p>
 * This exists to provide an adaptability layer between old properties support and the new mechanism introduced in 4.4.
 *
 * @since 4.5
 *
 * @deprecated Use {@link org.mule.runtime.config.internal.model.dsl.config.DefaultConfigurationProperty} instead.
 */
@Deprecated
public class DefaultConfigurationProperty extends org.mule.runtime.config.internal.model.dsl.config.DefaultConfigurationProperty
    implements ConfigurationProperty {

  public DefaultConfigurationProperty(Object source, String parentPath, String value) {
    super(source, parentPath, value);
  }

  @Override
  public Object getRawValue() {
    return super.getValue();
  }

}

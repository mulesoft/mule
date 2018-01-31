/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model.properties;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.component.Component;

/**
 * Represents a configuration attribute.
 *
 * @since 4.1
 */
@NoImplement
public interface ConfigurationProperty {

  /**
   * @return the source of this configuration attribute. For instance, it may be an {@link Component} if it's source was defined
   *         in the artifact configuration or it may be the deployment properties configured at deployment time.
   */
  Object getSource();

  /**
   * @return the plain configuration value without resolution. A configuration value may contain reference to other configuration
   *         attributes.
   */
  Object getRawValue();

  /**
   * @return the key of the configuration attribute to reference it.
   */
  String getKey();

}

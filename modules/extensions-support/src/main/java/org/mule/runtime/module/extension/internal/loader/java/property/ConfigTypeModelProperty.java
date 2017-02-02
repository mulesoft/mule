/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;


import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ModelProperty;

/**
 * An immutable model property which specifies that the owning {@link EnrichableModel} requires a configuration of a given
 * {@link #configType}
 *
 * @since 4.0
 */
public class ConfigTypeModelProperty implements ModelProperty {

  private final Class<?> configType;

  /**
   * Creates a new instance for the given {@code configType}
   *
   * @param configType
   */
  public ConfigTypeModelProperty(Class<?> configType) {
    this.configType = configType;
  }

  /**
   * @return the {@link {@link #configType }}
   */
  public Class<?> getConfigType() {
    return configType;
  }

  /**
   * {@inheritDoc}
   *
   * @return {@code configType}
   */
  @Override
  public String getName() {
    return "configType";
  }

  /**
   * {@inheritDoc}
   *
   * @return {@code false}
   */
  @Override
  public boolean isPublic() {
    return false;
  }
}

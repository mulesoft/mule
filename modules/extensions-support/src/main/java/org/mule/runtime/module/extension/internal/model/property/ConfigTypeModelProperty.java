/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.model.property;


import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.extension.api.introspection.EnrichableModel;
import org.mule.runtime.extension.api.introspection.ModelProperty;

/**
 * An immutable model property which specifies that the owning {@link EnrichableModel} requires a configuration of a given
 * {@link #configType}
 *
 * @since 4.0
 */
public final class ConfigTypeModelProperty implements ModelProperty {

  private final MetadataType configType;

  /**
   * Creates a new instance for the given {@code configType}
   *
   * @param configType
   */
  public ConfigTypeModelProperty(MetadataType configType) {
    this.configType = configType;
  }

  /**
   * @return the {@link {@link #configType }}
   */
  public MetadataType getConfigType() {
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
  public boolean isExternalizable() {
    return false;
  }
}

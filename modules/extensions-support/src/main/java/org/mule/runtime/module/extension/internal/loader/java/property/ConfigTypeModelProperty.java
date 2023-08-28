/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;


import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ModelProperty;

import java.lang.ref.WeakReference;

/**
 * An immutable model property which specifies that the owning {@link EnrichableModel} requires a configuration of a given
 * {@link #configType}
 *
 * @since 4.0
 */
public class ConfigTypeModelProperty implements ModelProperty {

  private final WeakReference<Class<?>> configType;

  /**
   * Creates a new instance for the given {@code configType}
   *
   * @param configType
   */
  public ConfigTypeModelProperty(Class<?> configType) {
    this.configType = new WeakReference<>(configType);
  }

  /**
   * @return the {@link {@link #configType }}
   */
  public Class<?> getConfigType() {
    return configType.get();
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

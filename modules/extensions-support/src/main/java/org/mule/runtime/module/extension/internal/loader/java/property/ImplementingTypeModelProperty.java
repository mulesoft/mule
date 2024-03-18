/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.ModelProperty;

import java.lang.ref.WeakReference;

/**
 * An immutable model property which indicates that the owning {@link EnrichableModel} was derived from a given {@link #type}
 *
 * @since 4.0
 */
public final class ImplementingTypeModelProperty implements ModelProperty {

  private final WeakReference<Class<?>> type;

  /**
   * Creates a new instance referencing the given {@code type}
   *
   * @param type a {@link Class} which defines the owning {@link EnrichableModel}
   */
  public ImplementingTypeModelProperty(Class<?> type) {
    this.type = new WeakReference<>(type);
  }

  /**
   * @return a {@link Class} which defines the owning {@link EnrichableModel}
   */
  public Class<?> getType() {
    return type.get();
  }

  /**
   * {@inheritDoc}
   *
   * @return {@code implementingType}
   */
  @Override
  public String getName() {
    return "implementingType";
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

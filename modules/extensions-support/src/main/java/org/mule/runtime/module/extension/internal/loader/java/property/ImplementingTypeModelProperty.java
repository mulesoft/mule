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
 * An immutable model property which indicates that the owning {@link EnrichableModel} was derived from a given {@link #type}
 *
 * @since 4.0
 */
public final class ImplementingTypeModelProperty implements ModelProperty {

  private final Class<?> type;

  /**
   * Creates a new instance referencing the given {@code type}
   *
   * @param type a {@link Class} which defines the owning {@link EnrichableModel}
   */
  public ImplementingTypeModelProperty(Class<?> type) {
    this.type = type;
  }

  /**
   * @return a {@link Class} which defines the owning {@link EnrichableModel}
   */
  public Class<?> getType() {
    return type;
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

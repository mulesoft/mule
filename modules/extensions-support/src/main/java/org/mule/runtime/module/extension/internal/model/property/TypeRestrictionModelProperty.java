/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.model.property;

import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.runtime.extension.api.introspection.EnrichableModel;
import org.mule.runtime.extension.api.introspection.ModelProperty;

/**
 * A custom model property to link an {@link EnrichableModel} to a certain {@link #type} which restricts it somehow.
 *
 * @param <T> generic type of the restriction {@link #type}
 * @since 4.0
 */
public final class TypeRestrictionModelProperty<T> implements ModelProperty {

  private final Class<T> type;

  public TypeRestrictionModelProperty(Class<T> type) {
    checkArgument(type != null, "cannot restrict to a null type");
    this.type = type;
  }

  public Class<T> getType() {
    return type;
  }

  /**
   * {@inheritDoc}
   *
   * @return {@code typeRestriction}
   */
  @Override
  public String getName() {
    return "typeRestriction";
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

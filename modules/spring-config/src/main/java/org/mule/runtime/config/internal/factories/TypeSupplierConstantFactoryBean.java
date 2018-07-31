/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import org.mule.runtime.core.internal.util.TypeSupplier;

/**
 * A specialization of {@link ConstantFactoryBean} which tests the value to be an instance of {@link TypeSupplier},
 * in which case {@link #getObjectType()} will return the supplied type.
 *
 * @since 4.2
 */
public class TypeSupplierConstantFactoryBean<T> extends ConstantFactoryBean<T> {

  private final Class<?> type;

  public TypeSupplierConstantFactoryBean(T value) {
    super(value);
    type = value instanceof TypeSupplier ? (Class<?>) ((TypeSupplier) value).getType() : value.getClass();
  }

  @Override
  public Class<?> getObjectType() {
    return type;
  }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.util;

import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetResult;

import java.lang.reflect.Field;

/**
 * An implementation of {@link ValueSetter} for assigning the value of a single, non grouped {@link ParameterModel}
 * <p/>
 * For performance reasons, it caches the setter to be used
 *
 * @since 3.7.0
 */
public final class SingleValueSetter implements ValueSetter {

  /**
   * The name of the {@link ParameterModel} which this instance sets
   */
  private final String parameterName;

  private final FieldSetter<Object, Object> fieldSetter;

  public SingleValueSetter(String parameterName, Field field) {
    this.parameterName = parameterName;
    this.fieldSetter = new FieldSetter<>(field);
  }

  /**
   * Sets the {@code resolverSetResult} value for the {@link #parameterName} into the {@link Field} supplied in the constructor
   *
   * @param target the object on which the value is being set
   * @param resolverSetResult a {@link ResolverSetResult} containing the value that corresponds to {@code parameter}
   */
  @Override
  public void set(Object target, ResolverSetResult resolverSetResult) {
    Object value = resolverSetResult.get(parameterName);
    if (value != null) {
      fieldSetter.set(target, value);
    }
  }
}

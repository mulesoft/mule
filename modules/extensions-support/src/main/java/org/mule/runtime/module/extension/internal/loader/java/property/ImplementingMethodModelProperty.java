/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.operation.OperationModel;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

/**
 * An immutable model property which indicates that the owning {@link OperationModel} was derived from a given {@link #method}
 *
 * @since 4.0
 */
public class ImplementingMethodModelProperty implements ModelProperty {

  private final WeakReference<Method> method;

  /**
   * Creates a new instance referencing the given {@code method}
   *
   * @param method a {@link Method} which defines the owning {@link OperationModel}
   * @throws IllegalArgumentException if {@code method} is {@code null}
   */
  public ImplementingMethodModelProperty(Method method) {
    checkArgument(method != null, "method cannot be null");
    this.method = new WeakReference<>(method);
  }

  /**
   * @return a {@link Method} which defines the owning {@link OperationModel}
   */
  public Method getMethod() {
    return method.get();
  }

  /**
   * {@inheritDoc}
   *
   * @return {@code implementingMethod}
   */
  @Override
  public String getName() {
    return "implementingMethod";
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

/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.module.extension.api.loader.java.type.FunctionContainerElement;
import org.mule.runtime.module.extension.api.loader.java.type.FunctionElement;

import java.lang.reflect.Method;

/**
 * {@link FunctionElement} implementation which works with Classes
 *
 * @since 4.1
 */
public class FunctionWrapper extends MethodWrapper<FunctionContainerElement> implements FunctionElement {

  public FunctionWrapper(Method method, ClassTypeLoader typeLoader) {
    super(method, typeLoader);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FunctionContainerElement getEnclosingType() {
    return new FunctionContainerWrapper(method.getDeclaringClass(), typeLoader);
  }
}

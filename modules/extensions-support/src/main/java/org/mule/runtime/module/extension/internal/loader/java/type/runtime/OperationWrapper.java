/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.module.extension.api.loader.java.type.OperationContainerElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationElement;

import java.lang.reflect.Method;

/**
 * {@link OperationElement} implementation which works with Classes
 *
 * @since 4.1
 */
public class OperationWrapper extends MethodWrapper<OperationContainerElement> implements OperationElement {

  public OperationWrapper(Method method, ClassTypeLoader typeLoader) {
    super(method, typeLoader);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OperationContainerElement getEnclosingType() {
    return new OperationContainerWrapper(method.getDeclaringClass(), typeLoader);
  }
}

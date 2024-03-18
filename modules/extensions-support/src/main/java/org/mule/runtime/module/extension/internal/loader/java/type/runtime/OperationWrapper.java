/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

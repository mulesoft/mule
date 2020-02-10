/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

public final class GeneratedInstance<T> {

  private final T instance;
  private final GeneratedClass<T> generatedClass;

  public GeneratedInstance(T instance, GeneratedClass<T> generatedClass) {
    this.instance = instance;
    this.generatedClass = generatedClass;
  }

  public T getInstance() {
    return instance;
  }

  public GeneratedClass<T> getGeneratedClass() {
    return generatedClass;
  }
}

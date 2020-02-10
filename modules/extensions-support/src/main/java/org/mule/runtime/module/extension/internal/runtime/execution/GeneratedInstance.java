/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

/**
 * Represents an instance of a {@link GeneratedClass}
 *
 * @param <T> the generic type of the generated instance
 * @since 4.3.0
 */
public final class GeneratedInstance<T> {

  private final T instance;
  private final GeneratedClass<T> generatedClass;

  /**
   * Creates a new instance
   *
   * @param instance       the instance
   * @param generatedClass the {@link GeneratedClass}
   */
  public GeneratedInstance(T instance, GeneratedClass<T> generatedClass) {
    this.instance = instance;
    this.generatedClass = generatedClass;
  }

  /**
   * @return The instance
   */
  public T getInstance() {
    return instance;
  }

  /**
   * @return The instance's {@link GeneratedClass}
   */
  public GeneratedClass<T> getGeneratedClass() {
    return generatedClass;
  }
}

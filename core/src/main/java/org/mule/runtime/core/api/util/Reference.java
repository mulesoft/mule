/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

/**
 * An object which holds a reference to a given {@link #value}.
 * <p>
 * Because this class represents a reference to such value instead of just being a simple holder, the {@link #equals(Object)} has
 * been redefined to test that the two values are actually the same ('{@code ==}'). Consistently, the {@link #hashCode()} method
 * is defined to return the value's identity hash code.
 * <p>
 * The above makes this class useful for cases in which you need to build a set of unique references, without depending on the
 * actual {@link #equals(Object)} and {@link #hashCode()} implementations.
 * <p>
 * {@code null} references are also supported.
 *
 * @param <T> the generic type of the referenced value
 */
public class Reference<T> {

  private final T value;

  /**
   * Creates a new instance
   *
   * @param value the referenced value
   */
  public Reference(T value) {
    this.value = value;
  }

  /**
   * @return the referenced value
   */
  public T get() {
    return value;
  }

  /**
   * @param obj the compared object
   * @return {@code true} if {@code obj} is a {@link Reference} which references the same value
   */
  @Override
  public boolean equals(Object obj) {
    return obj instanceof Reference && value == ((Reference) obj).value;
  }

  /**
   * @return the value's hash code
   */
  @Override
  public int hashCode() {
    return System.identityHashCode(value);
  }
}

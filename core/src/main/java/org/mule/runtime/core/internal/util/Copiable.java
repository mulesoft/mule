/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

/**
 * A component that knows how to create copies of itself.
 *
 * @param <T> the generic type of class implementing this interface
 * @since 3.8
 */
public interface Copiable<T> {

  /**
   * Creates a new copy of {@code this} instance.
   * <p/>
   * Each invocation to this method is expected to return a different instance. However, no guarantee is offered about the copy
   * being deep or shallow. That's up to each implementation.
   * <p/>
   * Implementations are expected to be thread-safe
   *
   * @return A new copy of {@code this} instance
   */
  T copy();
}

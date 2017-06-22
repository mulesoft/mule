/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

/**
 * A callback to notify about exceptions of a generic type
 * <p>
 * Implementations are to be reusable and thread-safe
 *
 * @param <E> the generic type of the exceptions to catch
 * @since 4.0
 */
public interface ExceptionCallback<E extends Throwable> {

  /**
   * Notifies that {@code exception} has occurred
   *
   * @param exception a {@code E}
   */
  void onException(E exception);
}

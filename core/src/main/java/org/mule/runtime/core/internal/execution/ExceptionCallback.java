/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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

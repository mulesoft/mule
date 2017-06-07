/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

/**
 * A {@link FunctionalInterface} which handles a random {@link Exception} by either returning a value of type {@code T} or by
 * throwing an exception of type {@code E}
 *
 * @param <T> the generic type of the return value
 * @param <E> the generic type of the expected exception
 * @since 4.0
 */
@FunctionalInterface
public interface ExceptionHandler<T, E extends Exception> {

  /**
   * Handles the given {@code exception} by either returning a new value of type {@code T} or throwing an {@link Exception} of
   * type {@code E}
   *
   * @param exception an {@link Exception}
   * @return a value of type {@code T}
   * @throws E if that's how the handler decides to handle the exception
   */
  T handle(Exception exception) throws E;
}

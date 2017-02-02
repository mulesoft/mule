/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

/**
 * Handles the result of asynchronous processing.
 *
 * @param <R> The type of the processing response value
 *
 * @since 1.0
 */
public interface CompletionHandler<R, E extends Throwable> {

  /**
   * Invoked on successful completion of asynchronous processing.
   * <p>
   * Exceptions found while processing the {@code result} are to be notified through
   * the {@code exceptionCallback}, which might (depending on {@code HandledCompletionExceptionResult})
   * produce a new value as the result of handling such error
   *
   * @param result            the result of processing
   * @param exceptionCallback handles errors processing the {@code result}
   */
  void onCompletion(R result, ExceptionCallback<Throwable> exceptionCallback);

  /**
   * Invoked when a failure occurs during asynchronous processing
   *
   * @param exception the exception thrown during processing
   */
  void onFailure(E exception);

}

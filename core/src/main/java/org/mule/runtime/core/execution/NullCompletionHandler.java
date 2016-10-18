/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

/**
 * Ignores the result of asynchronous processing
 *
 * @since 4.0
 */
public class NullCompletionHandler<R, E extends Throwable> implements CompletionHandler<R, E> {

  @Override
  public void onCompletion(R result, ExceptionCallback<Throwable> exceptionCallback) {
    // Nothing to do
  }

  @Override
  public void onFailure(E exception) {
    // Nothing to do
  }
}

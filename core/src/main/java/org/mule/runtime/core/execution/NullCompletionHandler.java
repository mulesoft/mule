/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import org.mule.runtime.api.execution.CompletionHandler;
import org.mule.runtime.api.execution.ExceptionCallback;

/**
 * Ignores the result of asynchronous processing
 *
 * @since 4.0
 */
public class NullCompletionHandler<Response, ProcessingException extends Throwable, HandledCompletionExceptionResult>
    implements CompletionHandler<Response, ProcessingException, HandledCompletionExceptionResult> {

  @Override
  public void onCompletion(Response result, ExceptionCallback<HandledCompletionExceptionResult, Exception> exceptionCallback) {
    // Nothing to do
  }

  @Override
  public void onFailure(ProcessingException exception) {
    // Nothing to do
  }
}

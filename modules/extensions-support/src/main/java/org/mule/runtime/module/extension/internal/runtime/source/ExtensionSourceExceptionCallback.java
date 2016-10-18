/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.execution.ExceptionCallback;
import org.mule.runtime.core.execution.ResponseCompletionCallback;

import java.util.function.Consumer;

/**
 * Channels exceptions through the
 * {@link ResponseCompletionCallback#responseSentWithFailure(MessagingException, Event)}.
 *
 * @since 4.0
 */
class ExtensionSourceExceptionCallback implements ExceptionCallback {

  private final ResponseCompletionCallback completionCallback;
  private final Event event;
  private final Consumer<MessagingException> errorResponseHandler;

  /**
   * Creates a new instance
   *
   * @param completionCallback    the callback used to send the failure response
   * @param event                 the related {@link MuleEvent}
   * @param errorResponseCallback a {@link Consumer} which acts as a callback for the {@link Event} which results
   *                              from handling exceptions
   */
  public ExtensionSourceExceptionCallback(ResponseCompletionCallback completionCallback, Event event,
                                          Consumer<MessagingException> errorResponseCallback) {
    this.completionCallback = completionCallback;
    this.event = event;
    this.errorResponseHandler = errorResponseCallback;
  }

  /**
   * Invokes {@link ResponseCompletionCallback#responseSentWithFailure(MessagingException, Event)} over
   * the {@link #completionCallback}, using the {@code exception} and {@link #event}
   *
   * @param exception a {@link Throwable}
   * @return a response {@link MuleEvent}
   */
  @Override
  public void onException(Throwable exception) {
    Event errorHandlingEvent = completionCallback.responseSentWithFailure(new MessagingException(event, exception), event);
    errorResponseHandler.accept(new MessagingException(errorHandlingEvent, exception));
  }
}

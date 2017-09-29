/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.mule.runtime.core.internal.util.InternalExceptionUtils.createErrorEvent;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.execution.ExceptionCallback;
import org.mule.runtime.core.internal.execution.ResponseCompletionCallback;
import org.mule.runtime.core.privileged.execution.MessageProcessContext;

import java.util.function.Consumer;

/**
 * Channels exceptions through the
 * {@link ResponseCompletionCallback#responseSentWithFailure(MessagingException, CoreEvent)}.
 *
 * @since 4.0
 */
class ExtensionSourceExceptionCallback implements ExceptionCallback {

  private final ResponseCompletionCallback completionCallback;
  private final CoreEvent event;
  private final Consumer<MessagingException> errorResponseHandler;
  private final MessageProcessContext messageProcessorContext;

  /**
   * Creates a new instance
   *
   * @param completionCallback the callback used to send the failure response
   * @param event the related {@link CoreEvent}
   * @param errorResponseCallback a {@link Consumer} which acts as a callback for the {@link CoreEvent} which results
   * @param messageProcessContext
   */
  public ExtensionSourceExceptionCallback(ResponseCompletionCallback completionCallback, CoreEvent event,
                                          Consumer<MessagingException> errorResponseCallback,
                                          MessageProcessContext messageProcessContext) {
    this.completionCallback = completionCallback;
    this.event = event;
    this.errorResponseHandler = errorResponseCallback;
    this.messageProcessorContext = messageProcessContext;
  }

  /**
   * Invokes {@link ResponseCompletionCallback#responseSentWithFailure(MessagingException, CoreEvent)} over the
   * {@link #completionCallback}, using the {@code exception} and {@link #event}
   *
   * @param exception a {@link Throwable}
   * @return a response {@link CoreEvent}
   */
  @Override
  public void onException(Throwable exception) {
    MessagingException messagingException = exception instanceof MessagingException ? (MessagingException) exception
        : new MessagingException(event, exception);
    CoreEvent errorEvent = createErrorEvent(event, messageProcessorContext.getMessageSource(), messagingException,
                                            messageProcessorContext.getErrorTypeLocator());
    messagingException.setProcessedEvent(errorEvent);
    CoreEvent errorHandlingEvent = completionCallback.responseSentWithFailure(messagingException, errorEvent);
    messagingException.setProcessedEvent(errorHandlingEvent);
    errorResponseHandler.accept(messagingException);
  }
}

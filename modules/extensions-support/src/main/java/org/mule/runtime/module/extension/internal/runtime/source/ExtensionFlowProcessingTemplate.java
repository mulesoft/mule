/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import org.mule.runtime.core.execution.CompletionHandler;
import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.execution.AsyncResponseFlowProcessingPhaseTemplate;
import org.mule.runtime.core.execution.ResponseCompletionCallback;

final class ExtensionFlowProcessingTemplate implements AsyncResponseFlowProcessingPhaseTemplate {

  private final Event event;
  private final Processor messageProcessor;
  private final CompletionHandler<Event, MessagingException> completionHandler;

  ExtensionFlowProcessingTemplate(Event event,
                                  Processor messageProcessor,
                                  CompletionHandler<Event, MessagingException> completionHandler) {
    this.event = event;
    this.messageProcessor = messageProcessor;
    this.completionHandler = completionHandler;
  }

  @Override
  public Event getEvent() throws MuleException {
    return event;
  }

  @Override
  public Event routeEvent(Event muleEvent) throws MuleException {
    return messageProcessor.process(muleEvent);
  }

  @Override
  public void sendResponseToClient(Event event, ResponseCompletionCallback responseCompletionCallback)
      throws MuleException {
    ExtensionSourceExceptionCallback exceptionCallback =
        new ExtensionSourceExceptionCallback(responseCompletionCallback, event, completionHandler::onFailure);
    runAndNotify(() -> completionHandler.onCompletion(event, exceptionCallback), this.event, responseCompletionCallback);
  }

  @Override
  public void sendFailureResponseToClient(MessagingException messagingException,
                                          ResponseCompletionCallback responseCompletionCallback)
      throws MuleException {
    runAndNotify(() -> completionHandler.onFailure(messagingException), event, responseCompletionCallback);
  }

  private void runAndNotify(Runnable runnable, MuleEvent event, ResponseCompletionCallback responseCompletionCallback) {
    try {
      runnable.run();
      responseCompletionCallback.responseSentSuccessfully();
    } catch (Exception e) {
      responseCompletionCallback.responseSentWithFailure(new MessagingException((Event) event, e),
                                                         (Event) event);
    }
  }
}

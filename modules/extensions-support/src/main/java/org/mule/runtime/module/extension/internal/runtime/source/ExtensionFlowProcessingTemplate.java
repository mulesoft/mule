/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import org.mule.runtime.api.execution.CompletionHandler;
import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.execution.AsyncResponseFlowProcessingPhaseTemplate;
import org.mule.runtime.core.execution.ResponseCompletionCallback;

final class ExtensionFlowProcessingTemplate implements AsyncResponseFlowProcessingPhaseTemplate {

  private final MuleEvent event;
  private final MessageProcessor messageProcessor;
  private final CompletionHandler<MuleEvent, MessagingException, MuleEvent> completionHandler;

  ExtensionFlowProcessingTemplate(MuleEvent event, MessageProcessor messageProcessor,
                                  CompletionHandler<MuleEvent, MessagingException, MuleEvent> completionHandler) {
    this.event = event;
    this.messageProcessor = messageProcessor;
    this.completionHandler = completionHandler;
  }

  @Override
  public org.mule.runtime.core.api.MuleEvent getMuleEvent() throws MuleException {
    return (org.mule.runtime.core.api.MuleEvent) event;
  }

  @Override
  public org.mule.runtime.core.api.MuleEvent routeEvent(org.mule.runtime.core.api.MuleEvent muleEvent) throws MuleException {
    return messageProcessor.process(muleEvent);
  }

  @Override
  public void sendResponseToClient(org.mule.runtime.core.api.MuleEvent muleEvent,
                                   ResponseCompletionCallback responseCompletionCallback)
      throws MuleException {
    ExtensionSourceExceptionCallback exceptionCallback =
        new ExtensionSourceExceptionCallback(responseCompletionCallback, muleEvent);
    runAndNotify(() -> completionHandler.onCompletion(muleEvent, exceptionCallback), event, responseCompletionCallback);
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
      responseCompletionCallback.responseSentWithFailure(e, (org.mule.runtime.core.api.MuleEvent) event);
    }
  }


}

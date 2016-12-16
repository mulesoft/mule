/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.execution.MessageProcessContext;
import org.mule.runtime.core.execution.ModuleFlowProcessingPhaseTemplate;
import org.mule.runtime.core.execution.ResponseCompletionCallback;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.publisher.BlockingSink;

final class ModuleFlowProcessingTemplate implements ModuleFlowProcessingPhaseTemplate {

  private final Message message;
  private final Processor messageProcessor;
  private final SourceCompletionHandler completionHandler;
  private final MessageProcessContext messageProcessorContext;
  private final Sink sink;

  ModuleFlowProcessingTemplate(Message message,
                               Processor messageProcessor,
                               SourceCompletionHandler completionHandler, MessageProcessContext messageProcessContext,
                               Sink sink) {
    this.message = message;
    this.messageProcessor = messageProcessor;
    this.completionHandler = completionHandler;
    this.messageProcessorContext = messageProcessContext;
    this.sink = sink;
  }

  @Override
  public Function<Event, Map<String, Object>> getSuccessfulExecutionResponseParametersFunction() {
    return (event -> completionHandler.createResponseParameters(event));
  }

  @Override
  public Function<Event, Map<String, Object>> getFailedExecutionResponseParametersFunction() {
    return (event -> completionHandler.createFailureResponseParameters(event));
  }

  @Override
  public Message getMessage() throws MuleException {
    return message;
  }

  @Override
  public Event routeEvent(Event muleEvent) throws MuleException {
    return messageProcessor.process(muleEvent);
  }

  @Override
  public Publisher<Event> dispatchEvent(Event muleEvent) {
    sink.accept(muleEvent);
    return muleEvent.getContext();
  }

  @Override
  public void sendResponseToClient(Event event, Map<String, Object> parameters,
                                   Function<Event, Map<String, Object>> errorResponseParametersFunction,
                                   ResponseCompletionCallback responseCompletionCallback) {
    Consumer<MessagingException> errorResponseCallback = (messagingException) -> {
      completionHandler.onFailure(messagingException, errorResponseParametersFunction.apply(messagingException.getEvent()));
    };
    ExtensionSourceExceptionCallback exceptionCallback =
        new ExtensionSourceExceptionCallback(responseCompletionCallback, event, errorResponseCallback, messageProcessorContext);
    runAndNotify(() -> completionHandler.onCompletion(event, parameters, exceptionCallback), event, responseCompletionCallback);
  }

  @Override
  public void sendFailureResponseToClient(MessagingException messagingException,
                                          Map<String, Object> parameters, ResponseCompletionCallback responseCompletionCallback) {
    runAndNotify(() -> completionHandler.onFailure(messagingException, parameters), messagingException.getEvent(),
                 responseCompletionCallback);
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

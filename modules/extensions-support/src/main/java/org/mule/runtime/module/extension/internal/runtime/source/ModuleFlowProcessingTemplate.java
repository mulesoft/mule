/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.mule.runtime.core.api.functional.Either.left;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.execution.MessageProcessContext;
import org.mule.runtime.core.execution.ModuleFlowProcessingPhaseTemplate;
import org.mule.runtime.core.execution.ResponseCompletionCallback;
import org.mule.runtime.core.api.util.func.CheckedConsumer;
import org.mule.runtime.core.api.util.func.CheckedFunction;
import org.mule.runtime.core.api.util.func.CheckedRunnable;

import java.util.Map;
import java.util.function.Function;

import org.reactivestreams.Publisher;

final class ModuleFlowProcessingTemplate implements ModuleFlowProcessingPhaseTemplate {

  private final Message message;
  private final Processor messageProcessor;
  private final SourceCompletionHandler completionHandler;
  private final MessageProcessContext messageProcessorContext;

  ModuleFlowProcessingTemplate(Message message,
                               Processor messageProcessor,
                               SourceCompletionHandler completionHandler, MessageProcessContext messageProcessContext) {
    this.message = message;
    this.messageProcessor = messageProcessor;
    this.completionHandler = completionHandler;
    this.messageProcessorContext = messageProcessContext;
  }

  @Override
  public CheckedFunction<Event, Map<String, Object>> getSuccessfulExecutionResponseParametersFunction() {
    return event -> completionHandler.createResponseParameters(event);
  }

  @Override
  public CheckedFunction<Event, Map<String, Object>> getFailedExecutionResponseParametersFunction() {
    return event -> completionHandler.createFailureResponseParameters(event);
  }

  @Override
  public Message getMessage() {
    return message;
  }

  @Override
  public Event routeEvent(Event muleEvent) throws MuleException {
    return messageProcessor.process(muleEvent);
  }

  @Override
  public Publisher<Event> routeEventAsync(Event event) {
    return just(event).transform(messageProcessor);
  }

  @Override
  public void sendResponseToClient(Event event, Map<String, Object> parameters,
                                   Function<Event, Map<String, Object>> errorResponseParametersFunction,
                                   ResponseCompletionCallback responseCompletionCallback) {
    runAndNotify(() -> completionHandler.onCompletion(event, parameters), event, responseCompletionCallback);
  }

  @Override
  public void sendFailureResponseToClient(MessagingException messagingException,
                                          Map<String, Object> parameters, ResponseCompletionCallback responseCompletionCallback) {
    runAndNotify(() -> completionHandler.onFailure(messagingException, parameters), messagingException.getEvent(),
                 responseCompletionCallback);
  }

  @Override
  public void sendAfterTerminateResponseToClient(Either<MessagingException, Event> either) {
    either.apply((CheckedConsumer<MessagingException>) messagingException -> completionHandler
                     .onTerminate(left(messagingException)),
                 (CheckedConsumer<Event>) event -> completionHandler.onTerminate(either));
  }

  private void runAndNotify(CheckedRunnable runnable, Event event, ResponseCompletionCallback responseCompletionCallback) {
    try {
      runnable.run();
      responseCompletionCallback.responseSentSuccessfully();
    } catch (Exception e) {
      responseCompletionCallback.responseSentWithFailure(new MessagingException(event, e), event);
    }
  }
}

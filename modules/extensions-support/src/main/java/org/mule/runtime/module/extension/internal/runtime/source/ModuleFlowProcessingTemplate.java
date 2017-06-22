/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.mule.runtime.core.api.functional.Either.left;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.util.func.CheckedConsumer;
import org.mule.runtime.core.api.util.func.CheckedFunction;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.internal.execution.ModuleFlowProcessingPhaseTemplate;
import org.mule.runtime.core.internal.execution.ResponseCompletionCallback;

import java.util.Map;
import java.util.function.Function;

import org.reactivestreams.Publisher;

final class ModuleFlowProcessingTemplate implements ModuleFlowProcessingPhaseTemplate {

  private final Message message;
  private final Processor messageProcessor;
  private final SourceCompletionHandler completionHandler;

  ModuleFlowProcessingTemplate(Message message,
                               Processor messageProcessor,
                               SourceCompletionHandler completionHandler) {
    this.message = message;
    this.messageProcessor = messageProcessor;
    this.completionHandler = completionHandler;
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
  public Publisher<Void> sendResponseToClient(Event event, Map<String, Object> parameters,
                                              Function<Event, Map<String, Object>> errorResponseParametersFunction,
                                              ResponseCompletionCallback responseCompletionCallback) {
    return from(completionHandler.onCompletion(event, parameters)).transform(notifyCompletion(event, responseCompletionCallback));
  }

  @Override
  public Publisher<Void> sendFailureResponseToClient(MessagingException messagingException,
                                                     Map<String, Object> parameters,
                                                     ResponseCompletionCallback responseCompletionCallback) {
    return from(completionHandler.onFailure(messagingException, parameters)).transform(notifyCompletion(
                                                                                                        messagingException
                                                                                                            .getEvent(),
                                                                                                        responseCompletionCallback));
  }

  @Override
  public void sendAfterTerminateResponseToClient(Either<MessagingException, Event> either) {
    either.apply((CheckedConsumer<MessagingException>) messagingException -> completionHandler
        .onTerminate(left(messagingException)),
                 (CheckedConsumer<Event>) event -> completionHandler.onTerminate(either));
  }

  private Function<Publisher<Void>, Publisher<Void>> notifyCompletion(Event event,
                                                                      ResponseCompletionCallback responseCompletionCallback) {
    return publisher -> from(publisher)
        .doOnSuccess(v -> responseCompletionCallback.responseSentSuccessfully())
        .doOnError(e -> responseCompletionCallback.responseSentWithFailure(new MessagingException(event, e), event));
  }
}

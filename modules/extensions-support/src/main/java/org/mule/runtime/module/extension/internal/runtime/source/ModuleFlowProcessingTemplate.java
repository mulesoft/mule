/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.mule.runtime.core.api.functional.Either.left;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.util.func.CheckedConsumer;
import org.mule.runtime.core.api.util.func.CheckedFunction;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.execution.ModuleFlowProcessingPhaseTemplate;
import org.mule.runtime.core.internal.execution.NotificationFunction;
import org.mule.runtime.core.internal.execution.SourceResultAdapter;

import java.util.List;
import java.util.Map;

import org.reactivestreams.Publisher;

final class ModuleFlowProcessingTemplate implements ModuleFlowProcessingPhaseTemplate {

  private final SourceResultAdapter sourceMessage;
  private final Processor messageProcessor;
  private final List<NotificationFunction> notificationFunctions;
  private final SourceCompletionHandler completionHandler;

  ModuleFlowProcessingTemplate(SourceResultAdapter sourceMessage,
                               Processor messageProcessor,
                               List<NotificationFunction> notificationFunctions, SourceCompletionHandler completionHandler) {
    this.sourceMessage = sourceMessage;
    this.messageProcessor = messageProcessor;
    this.notificationFunctions = notificationFunctions;
    this.completionHandler = completionHandler;
  }

  @Override
  public CheckedFunction<CoreEvent, Map<String, Object>> getSuccessfulExecutionResponseParametersFunction() {
    return completionHandler::createResponseParameters;
  }

  @Override
  public CheckedFunction<CoreEvent, Map<String, Object>> getFailedExecutionResponseParametersFunction() {
    return completionHandler::createFailureResponseParameters;
  }

  @Override
  public SourceResultAdapter getSourceMessage() {
    return sourceMessage;
  }

  @Override
  public List<NotificationFunction> getNotificationFunctions() {
    return notificationFunctions;
  }

  @Override
  public CoreEvent routeEvent(CoreEvent muleEvent) throws MuleException {
    return messageProcessor.process(muleEvent);
  }

  @Override
  public Publisher<CoreEvent> routeEventAsync(CoreEvent event) {
    return just(event).transform(messageProcessor);
  }

  @Override
  public Publisher<CoreEvent> routeEventAsync(Publisher<CoreEvent> eventPub) {
    return from(eventPub).transform(messageProcessor);
  }

  @Override
  public void sendResponseToClient(CoreEvent response, Map<String, Object> parameters, CompletableCallback<Void> callback) {
    completionHandler.onCompletion(response, parameters, callback);
  }

  @Override
  public void sendFailureResponseToClient(MessagingException exception,
                                          Map<String, Object> parameters,
                                          CompletableCallback<Void> callback) {
    completionHandler.onFailure(exception, parameters, callback);
  }

  @Override
  public void afterPhaseExecution(Either<MessagingException, CoreEvent> either) {
    either.apply((CheckedConsumer<MessagingException>) messagingException -> completionHandler
        .onTerminate(left(messagingException)),
                 (CheckedConsumer<CoreEvent>) event -> completionHandler.onTerminate(either));
  }

}

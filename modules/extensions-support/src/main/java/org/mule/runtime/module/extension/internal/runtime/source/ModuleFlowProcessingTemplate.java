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
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.util.func.CheckedConsumer;
import org.mule.runtime.core.api.util.func.CheckedFunction;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.execution.ModuleFlowProcessingPhaseTemplate;

import java.util.Map;

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
  public CheckedFunction<CoreEvent, Map<String, Object>> getSuccessfulExecutionResponseParametersFunction() {
    return completionHandler::createResponseParameters;
  }

  @Override
  public CheckedFunction<CoreEvent, Map<String, Object>> getFailedExecutionResponseParametersFunction() {
    return completionHandler::createFailureResponseParameters;
  }

  @Override
  public Message getMessage() {
    return message;
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
  public Publisher<Void> sendResponseToClient(CoreEvent response, Map<String, Object> parameters) {
    return completionHandler.onCompletion(response, parameters);
  }

  @Override
  public Publisher<Void> sendFailureResponseToClient(MessagingException messagingException,
                                                     Map<String, Object> parameters) {
    return completionHandler.onFailure(messagingException, parameters);
  }

  @Override
  public void afterPhaseExecution(Either<MessagingException, CoreEvent> either) {
    either.apply((CheckedConsumer<MessagingException>) messagingException -> completionHandler
        .onTerminate(left(messagingException)),
                 (CheckedConsumer<CoreEvent>) event -> completionHandler.onTerminate(either));
  }

}

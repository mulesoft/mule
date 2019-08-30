/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.source.scheduler;

import static java.util.Optional.empty;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.just;
import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.util.func.CheckedConsumer;
import org.mule.runtime.core.api.util.func.CheckedFunction;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.execution.FlowProcessTemplate;
import org.mule.runtime.core.internal.execution.NotificationFunction;
import org.mule.runtime.core.internal.execution.SourceResultAdapter;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.List;
import java.util.Map;

import org.reactivestreams.Publisher;

final class SchedulerFlowProcessingTemplate implements FlowProcessTemplate {

  private final Processor messageProcessor;
  private List<NotificationFunction> notificationFunctions;
  private DefaultSchedulerMessageSource defaultSchedulerMessageSource;
  private Message message;

  SchedulerFlowProcessingTemplate(Processor messageProcessor,
                                  List<NotificationFunction> notificationFunctions,
                                  DefaultSchedulerMessageSource defaultSchedulerMessageSource, Message message) {
    this.messageProcessor = messageProcessor;
    this.notificationFunctions = notificationFunctions;
    this.defaultSchedulerMessageSource = defaultSchedulerMessageSource;
    this.message = message;
  }

  @Override
  public CheckedFunction<CoreEvent, Map<String, Object>> getSuccessfulExecutionResponseParametersFunction() {
    return null;
  }

  @Override
  public CheckedFunction<CoreEvent, Map<String, Object>> getFailedExecutionResponseParametersFunction() {
    return null;
  }

  @Override
  public SourceResultAdapter getSourceMessage() {
    Result.Builder resultBuilder = Result.builder();
    resultBuilder
        .output(message.getPayload().getValue())
        .mediaType(message.getPayload().getDataType().getMediaType())
        .attributesMediaType(message.getAttributes().getDataType().getMediaType())
        .attributes(message.getAttributes());
    message.getPayload().getByteLength().ifPresent(length -> resultBuilder.length(length));
    return new SourceResultAdapter(resultBuilder.build(), null, message.getPayload().getDataType().getMediaType(), false,
                                   empty(), null);
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
    // Do nothing.
  }

  @Override
  public void sendFailureResponseToClient(MessagingException exception,
                                          Map<String, Object> parameters,
                                          CompletableCallback<Void> callback) {
    // Do nothing.
  }

  @Override
  public void afterPhaseExecution(Either<MessagingException, CoreEvent> either) {
    defaultSchedulerMessageSource.clearIsExecuting();
    either.apply(
                 (CheckedConsumer<MessagingException>) messagingException -> ((BaseEventContext) messagingException.getEvent()
                     .getContext()).error(messagingException),
                 (CheckedConsumer<CoreEvent>) event -> ((BaseEventContext) event.getContext()).success());
  }
}

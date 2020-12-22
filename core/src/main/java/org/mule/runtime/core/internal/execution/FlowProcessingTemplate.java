/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.util.func.CheckedFunction;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.sdk.api.runtime.operation.Result;

import java.util.List;
import java.util.Map;

import org.reactivestreams.Publisher;

/**
 * Abstract flow processing template with shared logic between all implementations.
 *
 * @since 4.3.0
 */
public abstract class FlowProcessingTemplate implements FlowProcessTemplate {

  private final Processor messageProcessor;
  private final List<NotificationFunction> notificationFunctions;

  protected FlowProcessingTemplate(Processor messageProcessor,
                                   List<NotificationFunction> notificationFunctions) {
    this.messageProcessor = messageProcessor;
    this.notificationFunctions = notificationFunctions;
  }

  @Override
  public CheckedFunction<CoreEvent, Map<String, Object>> getSuccessfulExecutionResponseParametersFunction() {
    return event -> emptyMap();
  }

  @Override
  public CheckedFunction<CoreEvent, Map<String, Object>> getFailedExecutionResponseParametersFunction() {
    return event -> emptyMap();
  }

  @Override
  public SourceResultAdapter getSourceMessage() {
    // payloadMediaTypeResolver is not needed since transmitted results is not a collection. Also, the cursorProviderFactory isn't
    // needed either since no value is be
    // ing communicated bu the ResultAdapter. This implies that no content is needed to be
    // streamed.
    return new SourceResultAdapter(Result.builder().build(), null, ANY, false,
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
    // Do nothing.
  }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import static java.util.Collections.emptyMap;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.just;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.util.func.CheckedFunction;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.reactivestreams.Publisher;

/**
 * Abstract {@link ModuleFlowProcessingPhaseTemplate} implementation, with shared logic between all implementations.
 *
 * @since 4.2.2
 */
public abstract class ModuleFlowProcessingTemplate implements ModuleFlowProcessingPhaseTemplate {

  private final Processor messageProcessor;
  private final List<NotificationFunction> notificationFunctions;

  protected ModuleFlowProcessingTemplate(Processor messageProcessor,
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
    // needed either since no value is being communicated bu the ResultAdapter. This implies that no content is needed to be
    // streamed.
    return new SourceResultAdapter(Result.builder().build(), null, ANY, false,
                                   Optional.empty(), null);
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
  public Publisher<Void> sendResponseToClient(CoreEvent response, Map<String, Object> parameters) {
    return empty();
  }

  @Override
  public Publisher<Void> sendFailureResponseToClient(MessagingException messagingException,
                                                     Map<String, Object> parameters) {
    return empty();
  }

  @Override
  public void afterPhaseExecution(Either<MessagingException, CoreEvent> either) {
    // Do nothing.
  }
}

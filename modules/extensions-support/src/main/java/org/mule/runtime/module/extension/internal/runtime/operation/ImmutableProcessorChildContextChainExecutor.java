/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.runtime.core.internal.event.DefaultEventContext.child;
import static org.mule.runtime.core.internal.event.EventQuickCopy.quickCopy;
import static org.mule.runtime.module.extension.api.runtime.privileged.EventedResult.from;

import static java.util.Optional.ofNullable;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.event.EventedResult;
import org.mule.runtime.core.privileged.exception.MessagingException;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.api.runtime.privileged.ChildContextChain;
import org.mule.runtime.module.extension.internal.runtime.execution.SdkInternalContext;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;


public class ImmutableProcessorChildContextChainExecutor implements ChildContextChain, ProcessorChainExecutor {

  private static final Logger LOGGER = getLogger(ImmutableProcessorChildContextChainExecutor.class);

  /**
   * Processor that will be executed upon calling process
   */
  private final MessageProcessorChain chain;

  /**
   * Event that will be cloned for dispatching
   */
  private final CoreEvent originalEvent;

  /**
   * original event context that will be cloned for dispatching
   */
  private final BaseEventContext oldContext;

  /**
   * Excutor to delegate the processing
   */
  private final ChainExecutor chainExecutor;

  private final ProcessorChainExecutor delegate;

  private final ComponentLocation location;

  /**
   *
   * @param streamingManager
   * @param event            the original {@link CoreEvent} for the execution of the given chain
   * @param chain            a {@link Processor} chain to be executed
   */
  public ImmutableProcessorChildContextChainExecutor(StreamingManager streamingManager, CoreEvent event,
                                                     MessageProcessorChain chain) {
    this.originalEvent = event;
    this.chain = chain;
    this.oldContext = (BaseEventContext) this.originalEvent.getContext();
    this.delegate = new ImmutableProcessorChainExecutor(streamingManager, this.originalEvent, this.chain);
    this.location = chain.getLocation();
    this.chainExecutor = new ChainExecutor(chain, originalEvent);
  }

  private void setSdkInternalContextValues(CoreEvent eventWithCorrelationId) {
    final String eventId = eventWithCorrelationId.getContext().getId();
    SdkInternalContext sdkInternalContext = SdkInternalContext.from(eventWithCorrelationId);
    sdkInternalContext.putContext(location, eventId);
  }

  private CoreEvent withPreviousCorrelationid(CoreEvent event) {
    return CoreEvent.builder(originalEvent).variables(event.getVariables()).message(event.getMessage()).build();
  }

  private EventedResult resultWithPreviousCorrelationId(EventedResult result) {
    return from(withPreviousCorrelationid(result.getEvent()));
  }

  private EventContext createCorrelationIdContext(String correlationId) {
    return child(oldContext, ofNullable(location), correlationId);
  }


  @Override
  public void process(final String correlationId, Consumer<Result> onSuccess, BiConsumer<Throwable, Result> onError) {
    CoreEvent eventWithCorrelationId = quickCopy(createCorrelationIdContext(correlationId), originalEvent);
    setSdkInternalContextValues(eventWithCorrelationId);

    LOGGER.debug("Changing event correlationId from '{}' to '{}' in location {}", originalEvent.getCorrelationId(), correlationId,
                 location.getLocation());
    chainExecutor.execute(eventWithCorrelationId,
                          result -> {
                            LOGGER.debug("Event with correlationId '{}' going back to '{}' (successful execution) in location {}",
                                         correlationId,
                                         originalEvent.getCorrelationId(), location.getLocation());
                            ((BaseEventContext) eventWithCorrelationId.getContext()).success(eventWithCorrelationId);
                            onSuccess.accept(resultWithPreviousCorrelationId((EventedResult) result));
                          }, (t, res) -> {
                            if (t instanceof MessagingException) {
                              t = new MessagingException(withPreviousCorrelationid(((MessagingException) t).getEvent()), t);
                            }
                            ((BaseEventContext) eventWithCorrelationId.getContext()).error(t);
                            LOGGER
                                .debug("Event with correlationId '{}' going back to '{}' (unsuccessful execution) in location {}",
                                       correlationId, originalEvent.getCorrelationId(), location.getLocation());
                            onError.accept(t, resultWithPreviousCorrelationId((EventedResult) res));
                          });
  }

  @Override
  public void process(Consumer<Result> onSuccess, BiConsumer<Throwable, Result> onError) {
    this.delegate.process(onSuccess, onError);
  }

  @Override
  public void process(Object payload, Object attributes, Consumer<Result> onSuccess, BiConsumer<Throwable, Result> onError) {
    this.delegate.process(payload, attributes, onSuccess, onError);
  }

  @Override
  public void process(Result input, Consumer<Result> onSuccess, BiConsumer<Throwable, Result> onError) {
    this.delegate.process(input, onSuccess, onError);
  }

  @Override
  public List<Processor> getMessageProcessors() {
    return chain.getMessageProcessors();
  }

  @Override
  public CoreEvent getOriginalEvent() {
    return originalEvent;
  }
}

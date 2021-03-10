/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.core.internal.event.DefaultEventContext.child;
import static org.mule.runtime.core.internal.event.EventQuickCopy.quickCopy;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.processor.chain.HasMessageProcessors;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.api.runtime.privileged.ChildContextChain;
import org.mule.runtime.module.extension.api.runtime.privileged.EventedResult;
import org.mule.runtime.module.extension.internal.runtime.execution.SdkInternalContext;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


public class ImmutableProcessorChildContextChainExecutor implements ChildContextChain, HasMessageProcessors {

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

  private ImmutableProcessorChainExecutor delegate;

  private ComponentLocation location;

  /**
   *
   * @param event the original {@link CoreEvent} for the execution of the given chain
   * @param chain a {@link Processor} chain to be executed
   */
  public ImmutableProcessorChildContextChainExecutor(CoreEvent event, MessageProcessorChain chain) {
    this.originalEvent = event;
    this.chain = chain;
    this.oldContext = (BaseEventContext) this.originalEvent.getContext();
    this.delegate = new ImmutableProcessorChainExecutor(this.originalEvent, this.chain);
    this.location = getLocation();
  }

  private ComponentLocation getLocation() {
    return chain.getLocation();
  }

  private void setSdkInternalContextValues(CoreEvent eventWithCorrelationId) {
    final String eventId = eventWithCorrelationId.getContext().getId();
    SdkInternalContext sdkInternalContext = SdkInternalContext.from(eventWithCorrelationId);
    sdkInternalContext.putContext(location, eventId);
    SdkInternalContext.OperationExecutionParams params =
        sdkInternalContext.getOperationExecutionParams(location, originalEvent.getContext().getId());
    sdkInternalContext.setOperationExecutionParams(location, eventId, params.getConfiguration(), params.getParameters(),
                                                   eventWithCorrelationId, params.getCallback(),
                                                   params.getExecutionContextAdapter());
  }

  private EventedResult resultWithPreviousCorrelationId(EventedResult result) {
    CoreEvent resultEvent = ((EventedResult) result).getEvent();
    CoreEvent resultWithouCorrelationId =
        CoreEvent.builder(originalEvent).variables(resultEvent.getVariables()).message(resultEvent.getMessage()).build();
    return EventedResult.from(resultWithouCorrelationId);
  }

  private EventContext createCorrelationIdContext(String correlationId) {
    BaseEventContext newContext = child(oldContext, ofNullable(location), correlationId);
    newContext.onComplete((ev, t) -> {
      if (ev != null) {
        oldContext.success(ev);
      } else {
        oldContext.error(t);
      }
    });
    return newContext;
  }


  @Override
  public void process(final String correlationId, Consumer<Result> onSuccess, BiConsumer<Throwable, Result> onError) {
    CoreEvent eventWithCorrelationId = quickCopy(createCorrelationIdContext(correlationId), originalEvent);
    setSdkInternalContextValues(eventWithCorrelationId);

    new ChainExecutor(chain, originalEvent, eventWithCorrelationId,
                      result -> onSuccess.accept(resultWithPreviousCorrelationId((EventedResult) result)),
                      (t, res) -> {
                        if (res instanceof EventedResult) {
                          onError.accept(t, resultWithPreviousCorrelationId((EventedResult) res));
                        } else {
                          onError.accept(t, res);
                        }
                      }).execute();
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
}

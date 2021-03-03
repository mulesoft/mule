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

import static java.util.Optional.of;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.processor.chain.HasMessageProcessors;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.api.runtime.privileged.ChildContextChain;

import java.util.List;
import java.util.Optional;
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
  }


  @Override
  public void process(String correlationId, Consumer<Result> onSuccess, BiConsumer<Throwable, Result> onError) {
    Optional<ComponentLocation> location = ofNullable(chain.getLocation());
    BaseEventContext newContext = child(oldContext, location, correlationId);
    CoreEvent eventWithCorrelationId = quickCopy(newContext, originalEvent);
    new ChainExecutor(chain, originalEvent, eventWithCorrelationId, onSuccess, onError).execute();
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

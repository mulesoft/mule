/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.core.internal.event.DefaultEventContext.child;
import static org.mule.runtime.core.internal.event.EventQuickCopy.quickCopy;
import static org.mule.runtime.module.extension.internal.runtime.execution.SdkInternalContext.from;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.processor.AbstractProcessor;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.processor.chain.HasMessageProcessors;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.api.runtime.privileged.ChildContextChain;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.execution.SdkInternalContext;

import java.util.List;
import java.util.Map;
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
    DefaultComponentLocation processorLocation =
        (DefaultComponentLocation) ((AbstractComponent) getMessageProcessors().get(0)).getLocation();
    List<DefaultComponentLocation.DefaultLocationPart> parts =
        processorLocation.getParts().subList(0, processorLocation.getParts().size() - 2).stream()
            .map(part -> (DefaultComponentLocation.DefaultLocationPart) part).collect(toList());
    return new DefaultComponentLocation(processorLocation.getName(), parts);
  }

  private void setSdkInternalContextValues(CoreEvent eventWithCorrelationId) {
    final String eventId = eventWithCorrelationId.getContext().getId();
    SdkInternalContext sdkInternalContext = from(eventWithCorrelationId);
    sdkInternalContext.putContext(location, eventId);
    SdkInternalContext.OperationExecutionParams params =
            sdkInternalContext.getOperationExecutionParams(location, originalEvent.getContext().getId());
    sdkInternalContext.setOperationExecutionParams(location, eventId, params.getConfiguration(), params.getParameters(),
            eventWithCorrelationId, params.getCallback(),
            params.getExecutionContextAdapter());

  }


  @Override
  public void process(final String correlationId, Consumer<Result> onSuccess, BiConsumer<Throwable, Result> onError) {
    BaseEventContext newContext = child(oldContext, ofNullable(location), correlationId);
    CoreEvent eventWithCorrelationId = quickCopy(newContext, originalEvent);
    setSdkInternalContextValues(eventWithCorrelationId);
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

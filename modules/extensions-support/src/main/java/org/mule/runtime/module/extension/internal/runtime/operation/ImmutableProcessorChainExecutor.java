/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.processor.chain.HasMessageProcessors;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.route.Chain;
import org.mule.runtime.module.extension.api.runtime.privileged.EventedResult;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * An implementation of {@link Chain} that wraps a {@link Processor} and allows to execute it
 *
 * @since 4.0
 */
public class ImmutableProcessorChainExecutor extends AbstractProcessorExecutor
    implements Chain, Initialisable, HasMessageProcessors {

  /**
   * Processor that will be executed upon calling process
   */
  private final MessageProcessorChain chain;

  /**
   * Creates a new immutable instance
   *
   * @param event the original {@link CoreEvent} for the execution of the given chain
   * @param chain a {@link Processor} chain to be executed
   */
  public ImmutableProcessorChainExecutor(CoreEvent event, MessageProcessorChain chain) {
    super(event, chain, chain.getLocation());
    this.chain = chain;
  }

  @Override
  public void process(Consumer<Result> onSuccess, BiConsumer<Throwable, Result> onError) {
    doProcess(onSuccess, onError);
  }

  @Override
  public void process(Object payload, Object attributes, Consumer<Result> onSuccess, BiConsumer<Throwable, Result> onError) {
    CoreEvent customEvent = CoreEvent.builder(originalEvent)
        .message(Message.builder()
            .payload(TypedValue.of(payload))
            .attributes(TypedValue.of(attributes))
            .build())
        .build();

    doProcess(customEvent, onSuccess, onError);
  }

  @Override
  public void process(Result result, Consumer<Result> onSuccess, BiConsumer<Throwable, Result> onError) {
    if (result instanceof EventedResult) {
      doProcess(((EventedResult) result).getEvent(), onSuccess, onError);
    } else {
      process(result.getOutput(), result.getAttributes(), onSuccess, onError);
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(chain);
  }

  @Override
  public List<Processor> getMessageProcessors() {
    return chain.getMessageProcessors();
  }

}

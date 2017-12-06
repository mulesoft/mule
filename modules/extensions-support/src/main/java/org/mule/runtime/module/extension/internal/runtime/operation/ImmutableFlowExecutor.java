/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.FlowExecutor;
import org.mule.runtime.extension.api.runtime.route.Chain;

import java.util.function.Consumer;

/**
 * An implementation of {@link Chain} that wraps a {@link Processor} and allows to execute it
 *
 * @since 4.1
 */
public class ImmutableFlowExecutor extends AbstractProcessorExecutor implements FlowExecutor {

  /**
   * Creates a new immutable instance
   *
   * @param event the original {@link CoreEvent} for the execution of the given chain
   // * @param chain a {@link Processor} chain to be executed
   */
  public ImmutableFlowExecutor(CoreEvent event, Flow flow) {
    super(event, flow, flow.getLocation());
  }

  @Override
  public void process(Consumer<Result> onSuccess, Consumer<Throwable> onError) {
    doProcess(originalEvent, onSuccess, (e, r) -> onError.accept(e));
  }

  @Override
  public void process(Object payload, Object attributes, Consumer<Result> onSuccess, Consumer<Throwable> onError) {
    CoreEvent customEvent = CoreEvent.builder(originalEvent)
        .message(Message.builder()
            .payload(TypedValue.of(payload))
            .attributes(TypedValue.of(attributes))
            .build())
        .build();

    doProcess(customEvent, onSuccess, (e, r) -> onError.accept(e));
  }

}

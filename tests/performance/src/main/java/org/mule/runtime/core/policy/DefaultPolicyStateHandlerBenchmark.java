/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.event.CoreEvent.builder;
import static org.mule.runtime.core.api.event.EventContextFactory.create;

import org.mule.AbstractBenchmark;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.api.policy.PolicyStateHandler;
import org.mule.runtime.core.api.policy.PolicyStateId;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.policy.DefaultPolicyStateHandler;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Threads;

public class DefaultPolicyStateHandlerBenchmark extends AbstractBenchmark {

  private PolicyStateHandler handler = new DefaultPolicyStateHandler();

  private Processor dummyProcessor = event -> event;

  @Benchmark
  @Threads(32)
  public Pair<Processor, Optional<CoreEvent>> source() {
    CoreEvent event = builder(create("" + Math.random(), "", CONNECTOR_LOCATION, NullExceptionHandler.getInstance()))
        .message(of(PAYLOAD)).build();

    PolicyStateId policyStateId = new PolicyStateId(event.getContext().getCorrelationId(), "myPolicy");

    handler.updateNextOperation(event.getContext().getCorrelationId(), dummyProcessor);
    handler.updateState(policyStateId, event);

    // execute-next
    final Processor nextOperation = handler.retrieveNextOperation(event.getContext().getCorrelationId());
    final Optional<CoreEvent> latestState = handler.getLatestState(policyStateId);

    ((BaseEventContext) event.getContext()).success();
    return Pair.of(nextOperation, latestState);
  }

  @Benchmark
  @Threads(32)
  public Pair<Processor, Optional<CoreEvent>> operation() {
    CoreEvent event = builder(create("" + Math.random(), "", CONNECTOR_LOCATION, NullExceptionHandler.getInstance()))
        .message(of(PAYLOAD)).build();

    PolicyStateId policyStateId = new PolicyStateId(event.getContext().getCorrelationId(), "myPolicy");

    Optional<CoreEvent> latestState = handler.getLatestState(policyStateId);
    handler.updateState(policyStateId, latestState.orElse(event));
    handler.updateNextOperation(event.getContext().getCorrelationId(), dummyProcessor);

    // execute-next
    final Processor nextOperation = handler.retrieveNextOperation(event.getContext().getCorrelationId());
    latestState = handler.getLatestState(policyStateId);

    handler.updateState(policyStateId, event);
    handler.updateState(policyStateId, event);

    ((BaseEventContext) event.getContext()).success();
    return Pair.of(nextOperation, latestState);
  }

}

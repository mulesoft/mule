/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.chain;

import static java.util.Optional.empty;
import static reactor.core.publisher.Flux.from;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.context.notification.FlowStackElement;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.context.notification.DefaultFlowCallStack;

import java.util.List;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * Constructs a custom chain for subflows using the subflow name as the chain name.
 */
public class SubflowMessageProcessorChainBuilder extends ExplicitMessageProcessorChainBuilder {

  protected MessageProcessorChain createInterceptingChain(Processor head, List<Processor> processors,
                                                          List<Processor> processorForLifecycle) {
    return new SubFlowMessageProcessorChain(name, head, processors, processorForLifecycle);
  }

  /**
   * Generates message processor identifiers specific for subflows.
   */
  static class SubFlowMessageProcessorChain extends ExplicitMessageProcessorChain {

    private String subFlowName;

    SubFlowMessageProcessorChain(String name, Processor head, List<Processor> processors,
                                 List<Processor> processorsForLifecycle) {
      super(name, empty(), head, processors, processorsForLifecycle);
      this.subFlowName = name;
    }

    private Consumer<Event> pushSubFlowFlowStackElement() {
      return event -> ((DefaultFlowCallStack) event.getFlowCallStack()).push(new FlowStackElement(subFlowName, null));
    }

    private Consumer<Event> popSubFlowFlowStackElement() {
      return event -> ((DefaultFlowCallStack) event.getFlowCallStack()).pop();
    }

    @Override
    public Publisher<Event> apply(Publisher<Event> publisher) {
      return from(publisher).concatMap(event -> Mono.just(event).doOnNext(pushSubFlowFlowStackElement())
          .transform(s -> super.apply(s)).doOnTerminate((event1, throwable) -> popSubFlowFlowStackElement().accept(event)));
    }
  }
}

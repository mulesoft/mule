/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.chain;

import static reactor.core.publisher.Flux.from;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.context.notification.FlowStackElement;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.context.notification.DefaultFlowCallStack;

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
    return new SubflowMessageProcessorChain(name, head, processors, processorForLifecycle);
  }

  /**
   * Generates message processor identfiers specific for subflows.
   */
  static class SubflowMessageProcessorChain extends ExplicitMessageProcessorChain implements SubFlowMessageProcessor {

    private String subFlowName;

    SubflowMessageProcessorChain(String name, Processor head, List<Processor> processors,
                                 List<Processor> processorsForLifecycle) {
      super(name, head, processors, processorsForLifecycle);
      this.subFlowName = name;
    }

    @Override
    public Event process(Event event) throws MuleException {
      pushSubFlowFlowStackElement().accept(event);

      try {
        return super.process(event);
      } finally {
        popSubFlowFlowStackElement().accept(event);
      }
    }

    private Consumer<Event> pushSubFlowFlowStackElement() {
      return event -> ((DefaultFlowCallStack) event.getFlowCallStack()).push(new FlowStackElement(getSubFlowName(), null));
    }

    private Consumer<Event> popSubFlowFlowStackElement() {
      return event -> ((DefaultFlowCallStack) event.getFlowCallStack()).pop();
    }

    @Override
    public Publisher<Event> apply(Publisher<Event> publisher) {
      return from(publisher).concatMap(event -> Mono.just(event).doOnNext(pushSubFlowFlowStackElement())
          .transform(s -> super.apply(s)).doOnTerminate((event1, throwable) -> popSubFlowFlowStackElement().accept(event)));
    }

    @Override
    public String getSubFlowName() {
      return subFlowName;
    }
  }
}

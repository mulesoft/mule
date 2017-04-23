/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor;

import static org.mule.runtime.core.DefaultEventContext.child;
import static org.mule.runtime.core.api.Event.builder;
import static org.mule.runtime.core.api.rx.Exceptions.rxExceptionToMuleException;
import static org.mule.runtime.core.processor.chain.ExplicitMessageProcessorChainBuilder.*;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.processor.chain.ExplicitMessageProcessorChainBuilder;

import java.util.List;

import org.reactivestreams.Publisher;

/**
 * Some convenience methods for message processors.
 */
public class MessageProcessors {

  private MessageProcessors() {
    // do not instantiate
  }

  /**
   * Creates a new {@link MessageProcessorChain} from one or more {@link Processor}'s. Note that this performs chains construction
   * but wil not inject {@link MuleContext} or {@link FlowConstruct} or perform any lifecycle.
   *
   * @param processors processors to construct chains from.
   * @return new {@link MessageProcessorChain} instance.
   */
  public static MessageProcessorChain newChain(Processor... processors) {
    if (processors.length == 1 && processors[0] instanceof MessageProcessorChain
        && !(processors[0] instanceof ExplicitMessageProcessorChain)) {
      return (MessageProcessorChain) processors[0];
    } else {
      return new DefaultMessageProcessorChainBuilder().chain(processors).build();
    }
  }

  /**
   * Creates a new {@link MessageProcessorChain} from a {@link List} of {@link Processor}'s. Note that this performs chains
   * construction but wil not inject {@link MuleContext} or {@link FlowConstruct} or perform any lifecycle.
   *
   * @param processors list of processors to construct chains from.
   * @return new {@link MessageProcessorChain} instance.
   */
  public static MessageProcessorChain newChain(List<Processor> processors) {
    if (processors.size() == 1 && processors.get(0) instanceof MessageProcessorChain
        && !(processors.get(0) instanceof ExplicitMessageProcessorChain)) {
      return (MessageProcessorChain) processors.get(0);
    } else {
      return new DefaultMessageProcessorChainBuilder().chain(processors).build();
    }
  }

  /**
   * Creates a new explicit {@link MessageProcessorChain} from one or more {@link Processor}'s. Note that this performs chains
   * construction but wil not inject {@link MuleContext} or {@link FlowConstruct} or perform any lifecycle.
   *
   * @param processors list of processors to construct chains from.
   * @return new {@link MessageProcessorChain} instance.
   */
  public static MessageProcessorChain newExplicitChain(Processor... processors) {
    if (processors.length == 1 && processors[0] instanceof ExplicitMessageProcessorChain) {
      return (MessageProcessorChain) processors[0];
    } else {
      return new ExplicitMessageProcessorChainBuilder().chain(processors).build();
    }
  }

  /**
   * Creates a new explicit {@link MessageProcessorChain} from a {@link List} of {@link Processor}'s. Note that this performs
   * chains construction but wil not inject {@link MuleContext} or {@link FlowConstruct} or perform any lifecycle.
   *
   * @param processors list of processors to construct chains from.
   * @return new {@link MessageProcessorChain} instance.
   */
  public static MessageProcessorChain newExplicitChain(List<Processor> processors) {
    if (processors.size() == 1 && processors.get(0) instanceof ExplicitMessageProcessorChain) {
      return (MessageProcessorChain) processors.get(0);
    } else {
      return new ExplicitMessageProcessorChainBuilder().chain(processors).build();
    }
  }

  public static Event processToApply(Event event, ReactiveProcessor processor) throws MuleException {
    try {
      return just(event).transform(processor).block();
    } catch (Throwable e) {
      throw rxExceptionToMuleException(e);
    }
  }

  public static Event processToApply(Event event, Flow flow) throws MuleException {
    try {
      just(event)
          .transform(flow)
          // Use empty error handler to avoid reactor ErrorCallbackNotImplemented
          .subscribe(null, throwable -> {
          });
      return from(event.getContext().getResponsePublisher()).block();
    } catch (Throwable e) {
      throw rxExceptionToMuleException(e);
    }
  }

  /**
   * Process a {@link ReactiveProcessor} using a child {@link EventContext}. This is useful if it is necessary to performing
   * processing in a scope and handle an empty result rather than complete the response for the whole Flow.
   *
   * @param event the event to process.
   * @param processor the processor to process.
   * @return the future result of processing processor.
   */
  public static Publisher<Event> processWithChildContext(Event event, ReactiveProcessor processor) {
    EventContext child = child(event.getContext());
    just(Event.builder(child, event).build())
        .transform(processor)
        .subscribe(result -> child.success(result),
                   throwable -> child.error(throwable));
    return from(child.getResponsePublisher())
        .map(result -> Event.builder(event.getContext(), result).build())
        .doOnError(MessagingException.class, me -> me.setProcessedEvent(builder(event.getContext(), me.getEvent()).build()));
  }

}

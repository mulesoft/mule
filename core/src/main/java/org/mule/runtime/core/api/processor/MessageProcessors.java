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
import static org.mule.runtime.core.internal.util.rx.Operators.requestUnbounded;
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
import org.mule.runtime.core.processor.chain.ExplicitMessageProcessorChainBuilder.ExplicitMessageProcessorChain;

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

  /**
   * Adapt a {@link ReactiveProcessor} used via non-blocking API {@link ReactiveProcessor#apply(Object)} by blocking and waiting
   * for response {@link Event} or throwing an {@link MuleException} in the case of an error.
   * <p/>
   * A plain {@link ReactiveProcessor} does not handle {@link EventContext} completion or error handling so this method simply
   * uses {@code just(event).transform(processor).block();}
   *
   * @param event event to process.
   * @param processor processor to adapt.
   * @return result event
   * @throws MuleException
   */
  public static Event processToApply(Event event, ReactiveProcessor processor) throws MuleException {
    if (processor instanceof Flow) {
      return processToApply(event, (Flow) processor);
    } else if (processor instanceof MessageProcessorChain) {
      return processToApply(event, (MessageProcessorChain) processor);
    } else {
      try {
        return just(event).transform(processor).block();
      } catch (Throwable e) {
        throw rxExceptionToMuleException(e);
      }
    }
  }

  /**
   * Adapt a {@link MessageProcessorChain} used via non-blocking API {@link ReactiveProcessor#apply(Object)} by blocking and
   * waiting for response {@link Event} or throwing an {@link MuleException} in the case of an error.
   * <p/>
   * A {@link MessageProcessorChain} does not handle {@link EventContext} completion but does do error handling so this method
   * manually completes response and then blocks on {@link EventContext} response {@link Publisher}.
   *
   * @param event event to process.
   * @param messageProcessorChain processor chain to adapt.
   * @return result event
   * @throws MuleException
   */
  public static Event processToApply(Event event, MessageProcessorChain messageProcessorChain) throws MuleException {
    try {
      just(event)
          .transform(messageProcessorChain)
          .subscribe(response -> event.getContext().success(response), throwable -> event.getContext().error(throwable));
      return from(event.getContext().getResponsePublisher()).block();
    } catch (Throwable e) {
      throw rxExceptionToMuleException(e);
    }
  }

  /**
   * Adapt a {@link Flow} used via non-blocking API {@link ReactiveProcessor#apply(Object)} by blocking and waiting for response
   * {@link Event} or throwing an {@link MuleException} in the case of an error.
   * <p/>
   * A {@link Flow} handles {@link EventContext} completion and error handling so this method dispatches {@link Event} to
   * {@link Flow} and then blocks on {@link EventContext} response {@link Publisher}.
   *
   * @param event event to process.
   * @param flow flow to adapt.
   * @return result event
   * @throws MuleException
   */
  public static Event processToApply(Event event, Flow flow) throws MuleException {
    try {
      just(event)
          .transform(flow)
          .subscribe(requestUnbounded());
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
        .subscribe(child::success, child::error);
    return from(child.getResponsePublisher())
        .map(result -> Event.builder(event.getContext(), result).build())
        .doOnError(MessagingException.class, me -> me.setProcessedEvent(builder(event.getContext(), me.getEvent()).build()));
  }

}

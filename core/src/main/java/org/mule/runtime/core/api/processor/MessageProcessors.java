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
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.EventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
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
   * If the {@link ReactiveProcessor} drops the event due to an error or stop action, then the result of returned will be that of
   * the {@link EventContext} completion. Attempting to adapt a processor implementation that filters events and does not complete
   * the {@link EventContext} will cause this method to never return.
   *
   * @param event event to process.
   * @param processor processor to adapt.
   * @return result event
   * @throws MuleException
   */
  public static Event processToApply(Event event, ReactiveProcessor processor) throws MuleException {
    try {
      return just(event).transform(processor).switchIfEmpty(from(event.getContext().getResponsePublisher())).block();
    } catch (Throwable e) {
      throw rxExceptionToMuleException(e);
    }
  }

  /**
   * Process a {@link ReactiveProcessor} using a child {@link EventContext}. This is useful if it is necessary to perform
   * processing in a scope and handle an empty result or error locally rather than complete the response for the whole Flow.
   * <p>
   * No error-handling will be performed when errors occur.
   *
   * @param event the event to process.
   * @param processor the processor to process.
   * @param componentLocation
   * @return the future result of processing processor.
   */
  public static Publisher<Event> processWithChildContext(Event event, ReactiveProcessor processor,
                                                         ComponentLocation componentLocation) {
    return internalProcessWithChildContext(event, processor, child(event.getContext(), componentLocation, false));
  }

  /**
   * Process a {@link ReactiveProcessor} using a child {@link EventContext}. This is useful if it is necessary to perform
   * processing in a scope and handle an empty result or error locally rather than complete the response for the whole Flow.
   * <p>
   * The {@link MessagingExceptionHandler} configured on {@link MessageProcessorChain} or {@link FlowConstruct} will be used to
   * handle any errors that occur.
   *
   * @param event the event to process.
   * @param processor the processor to process.
   * @param componentLocation
   * @return the future result of processing processor.
   */
  public static Publisher<Event> processWithChildContextHandleErrors(Event event, ReactiveProcessor processor,
                                                                     ComponentLocation componentLocation) {
    return internalProcessWithChildContext(event, processor, child(event.getContext(), componentLocation, true));
  }

  private static Publisher<Event> internalProcessWithChildContext(Event event, ReactiveProcessor processor, EventContext child) {
    return just(builder(child, event).build())
        .transform(processor)
        .doOnNext(result -> {
          if (!(from(child.getResponsePublisher()).toFuture().isDone())) {
            child.success(result);
          }
        })
        .switchIfEmpty(from(child.getResponsePublisher()))
        .map(result -> builder(event.getContext(), result).build())
        .doOnError(MessagingException.class, me -> me.setProcessedEvent(builder(event.getContext(), me.getEvent()).build()))
        .doOnSuccess(result -> {
          if (result == null) {
            event.getContext().success();
          }
        });
  }

}

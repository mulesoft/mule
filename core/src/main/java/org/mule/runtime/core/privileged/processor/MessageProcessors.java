/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static org.mule.runtime.core.api.event.CoreEvent.builder;
import static org.mule.runtime.core.api.rx.Exceptions.rxExceptionToMuleException;
import static org.mule.runtime.core.internal.event.DefaultEventContext.child;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import org.reactivestreams.Publisher;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import reactor.core.publisher.Mono;

/**
 * Some convenience methods for message processors.
 */
public class MessageProcessors {

  private MessageProcessors() {
    // do not instantiate
  }

  /**
   * Creates a new {@link MessageProcessorChain} from a {@link List} of {@link Processor}'s. Note that this performs chains
   * construction but will not inject {@link MuleContext} or perform any lifecycle.
   *
   * @param processingStrategy the processing strategy to use for configuring the chain. It may be {@link Optional#empty()}.
   * @param processors list of processors to construct chains from.
   * @return new {@link MessageProcessorChain} instance.
   */
  public static MessageProcessorChain newChain(Optional<ProcessingStrategy> processingStrategy, List<Processor> processors) {
    if (processors.size() == 1 && processors.get(0) instanceof MessageProcessorChain) {
      return (MessageProcessorChain) processors.get(0);
    } else {
      DefaultMessageProcessorChainBuilder defaultMessageProcessorChainBuilder = new DefaultMessageProcessorChainBuilder();
      processingStrategy.ifPresent(defaultMessageProcessorChainBuilder::setProcessingStrategy);
      return defaultMessageProcessorChainBuilder.chain(processors).build();
    }
  }

  /**
   * Creates a new {@link MessageProcessorChain} from a {@link List} of {@link Processor}'s. Note that this performs chains
   * construction but will not inject {@link MuleContext} or perform any lifecycle.
   *
   * @param processingStrategy the processing strategy to use for configuring the chain. It may be {@link Optional#empty()}.
   * @param processors list of processors to construct chains from.
   * @return new {@link MessageProcessorChain} instance.
   */
  public static MessageProcessorChain newChain(Optional<ProcessingStrategy> processingStrategy, Processor... processors) {
    return newChain(processingStrategy, asList(processors));
  }

  /**
   * Adapt a {@link ReactiveProcessor} that implements {@link ReactiveProcessor#apply(Object)} to a blocking API that blocks until
   * a {@link CoreEvent} result is available or throws an exception in the case of an error. This is currently used widely to
   * continue to support the blocking {@link Processor#process(CoreEvent)} API when the implementation of a {@link Processor} is
   * implemented via {@link ReactiveProcessor#apply(Object)}.
   * <p>
   * The {@link CoreEvent} returned by this method will <b>not</b> be completed with a {@link CoreEvent} or {@link Throwable}
   * unless the delegate {@link Processor} does this.
   *
   * TODO MULE-13054 Remove blocking processor API
   *
   * @param event event to process
   * @param processor processor to adapt
   * @return result event
   * @throws MuleException if an error occurs
   */
  public static CoreEvent processToApply(CoreEvent event, ReactiveProcessor processor) throws MuleException {
    return processToApply(event, processor, false);
  }

  /**
   * Adapt a {@link ReactiveProcessor} that implements {@link ReactiveProcessor#apply(Object)} to a blocking API that blocks until
   * a {@link CoreEvent} result is available or throws an exception in the case of an error. This is currently used widely to
   * continue to support the blocking {@link Processor#process(CoreEvent)} API when the implementation of a {@link Processor} is
   * implemented via {@link ReactiveProcessor#apply(Object)}.
   * <p>
   * The {@link CoreEvent} returned by this method will be completed with a {@link CoreEvent} or {@link Throwable} if
   * {@code completeContext} is {@code true}.
   *
   * TODO MULE-13054 Remove blocking processor API
   *
   * @param event event to process
   * @param processor processor to adapt
   * @return result event
   * @throws MuleException if an error occurs
   */
  public static CoreEvent processToApply(CoreEvent event, ReactiveProcessor processor, boolean completeContext)
      throws MuleException {
    try {
      return just(event)
          .transform(processor)
          // Ensure errors handled by MessageProcessorChains are returned
          .switchIfEmpty(from(((BaseEventContext) event.getContext()).getResponsePublisher()))
          .doOnSuccess(completeSuccessIfNeeded((event.getContext()), completeContext))
          .doOnError(completeErrorIfNeeded((event.getContext()), completeContext))
          .block();
    } catch (Throwable e) {
      throw rxExceptionToMuleException(e);
    }
  }

  /**
   * Adapt a {@link ReactiveProcessor} that implements {@link ReactiveProcessor#apply(Object)} to a blocking API that blocks until
   * a {@link CoreEvent} result is available or throws an exception in the case of an error. This method differs from
   * {@link #processToApply(CoreEvent, ReactiveProcessor)} in that processing will be scoped using a child {@link EventContext}
   * such that completion of the {@link EventContext} in the delegate {@link Processor} does not complete the original
   * {@link EventContext}.
   *
   * @param event event to process
   * @param processor processor to adapt
   * @return result event
   * @throws MuleException if an error occurs
   */
  public static CoreEvent processToApplyWithChildContext(CoreEvent event, ReactiveProcessor processor)
      throws MuleException {
    try {
      return just(event)
          .transform(publisher -> from(publisher).flatMap(request -> Mono
              .from(internalProcessWithChildContext(request, processor,
                                                    newChildContext(event, empty()), false))))
          .block();
    } catch (Throwable e) {
      throw rxExceptionToMuleException(e);
    }
  }

  /**
   * Process an {@link CoreEvent} with a given {@link ReactiveProcessor} returning a {@link Publisher<CoreEvent>} via which the
   * future {@link CoreEvent} or {@link Throwable} will be published.
   * <p/>
   * The {@link CoreEvent} returned by this method <b>will</b> be completed with the same {@link CoreEvent} instance and if an
   * error occurs during processing the {@link EventContext} will be completed with the error.
   *
   * @param event event to process
   * @param processor processor to use
   * @return future result
   */
  public static Publisher<CoreEvent> process(CoreEvent event, ReactiveProcessor processor) {
    return just(event).transform(processor)
        .onErrorMap(t -> !(t instanceof MessagingException), t -> {
          if (processor instanceof Component) {
            return new MessagingException(event, t, (Component) processor);
          } else {
            return new MessagingException(event, t);
          }
        })
        .switchIfEmpty(from(((BaseEventContext) event.getContext()).getResponsePublisher()))
        .doOnSuccess(completeSuccessIfNeeded((event.getContext()), true))
        .doOnError(completeErrorIfNeeded((event.getContext()), true));
  }

  /**
   * Process a {@link ReactiveProcessor} using a child {@link BaseEventContext}. This is useful if it is necessary to perform
   * processing in a scope and handle an empty result or error locally rather than complete the response for the whole Flow.
   *
   * @param event the event to process.
   * @param processor the processor to process.
   * @param componentLocation
   * @return the future result of processing processor.
   */
  public static Publisher<CoreEvent> processWithChildContext(CoreEvent event, ReactiveProcessor processor,
                                                             Optional<ComponentLocation> componentLocation) {
    return internalProcessWithChildContext(event, processor,
                                           newChildContext(event, componentLocation), true);
  }

  /**
   * Creates a new {@link BaseEventContext} which is child of the one in the given {@code event}
   * @param event the parent event
   * @param componentLocation the location of the component creating the child context
   * @return a child {@link BaseEventContext}
   */
  public static BaseEventContext newChildContext(CoreEvent event, Optional<ComponentLocation> componentLocation) {
    return child(((BaseEventContext) event.getContext()), componentLocation);
  }

  public static Publisher<CoreEvent> processWithChildContext(CoreEvent event, ReactiveProcessor processor,
                                                             BaseEventContext childContext) {
    return internalProcessWithChildContext(event, processor, childContext, true);
  }

  /**
   * Process a {@link ReactiveProcessor} using a child {@link EventContext}. This is useful if it is necessary to perform
   * processing in a scope and handle an empty result or error locally rather than complete the response for the whole Flow.
   * <p>
   * The {@link FlowExceptionHandler} configured on {@link MessageProcessorChain} or {@link FlowConstruct} will be used to handle
   * any errors that occur.
   *
   * @param event the event to process.
   * @param processor the processor to process.
   * @param componentLocation
   * @param exceptionHandler used to handle {@link Exception}'s.
   * @return the future result of processing processor.
   */
  public static Publisher<CoreEvent> processWithChildContext(CoreEvent event, ReactiveProcessor processor,
                                                             Optional<ComponentLocation> componentLocation,
                                                             FlowExceptionHandler exceptionHandler) {
    return internalProcessWithChildContext(event, processor,
                                           child(((BaseEventContext) event.getContext()), componentLocation,
                                                 exceptionHandler),
                                           true);
  }

  private static Publisher<CoreEvent> internalProcessWithChildContext(CoreEvent event, ReactiveProcessor processor,
                                                                      EventContext child, boolean completeParentOnEmpty) {
    return just(builder(child, event).build())
        .transform(processor)
        .doOnNext(completeSuccessIfNeeded(child, true))
        .switchIfEmpty(from(((BaseEventContext) child).getResponsePublisher()))
        .map(result -> builder(event.getContext(), result).build())
        .doOnError(MessagingException.class,
                   me -> me.setProcessedEvent(builder(event.getContext(), me.getEvent()).build()))
        .doOnSuccess(result -> {
          if (result == null && completeParentOnEmpty) {
            ((BaseEventContext) event.getContext()).success();
          }
        });
  }

  private static Consumer<CoreEvent> completeSuccessIfNeeded(EventContext child, boolean complete) {
    return result -> {
      if (!(from(((BaseEventContext) child).getResponsePublisher()).toFuture().isDone()) && complete) {
        ((BaseEventContext) child).success(result);
      }
    };
  }

  private static Consumer<Throwable> completeErrorIfNeeded(EventContext child, boolean complete) {
    return throwable -> {
      if (!(from(((BaseEventContext) child).getResponsePublisher()).toFuture().isDone()) && complete) {
        ((BaseEventContext) child).error(throwable);
      }
    };
  }

  /**
   * Helper method to get the {@link ProcessingStrategy} from a component.
   *
   * @param locator the locator
   * @param rootContainerLocation the component root container element
   * @return the processing strategy of the root component if it was an instance of {@link FlowConstruct}, empty otherwise.
   */
  public static Optional<ProcessingStrategy> getProcessingStrategy(ConfigurationComponentLocator locator,
                                                                   Location rootContainerLocation) {
    return locator.find(rootContainerLocation).filter(loc -> loc instanceof FlowConstruct)
        .map(loc -> ((FlowConstruct) loc).getProcessingStrategy());
  }
}

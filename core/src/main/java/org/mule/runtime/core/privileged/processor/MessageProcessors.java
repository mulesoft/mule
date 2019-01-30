/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static org.mule.runtime.core.api.rx.Exceptions.rxExceptionToMuleException;
import static org.mule.runtime.core.internal.event.DefaultEventContext.child;
import static org.mule.runtime.core.internal.event.EventQuickCopy.quickCopy;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

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
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;

import reactor.core.publisher.Flux;

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
    return processToApply(event, processor, completeContext,
                          from(((BaseEventContext) event.getContext()).getResponsePublisher()));
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
   * @param alternate publisher to use if {@code processor} doesn't return data.
   * @return result event
   * @throws MuleException if an error occurs
   */
  public static CoreEvent processToApply(CoreEvent event, ReactiveProcessor processor, boolean completeContext,
                                         Publisher<CoreEvent> alternate)
      throws MuleException {
    try {
      return just(event)
          .transform(processor)
          // Ensure errors handled by MessageProcessorChains are returned
          .switchIfEmpty(from(alternate))
          .doOnSuccess(completeSuccessIfNeeded((event.getContext()), completeContext))
          .doOnError(completeErrorIfNeeded((event.getContext()), completeContext))
          .block();
    } catch (Throwable e) {
      MuleException muleException = rxExceptionToMuleException(e);
      if (e.getCause() instanceof InterruptedException) {
        currentThread().interrupt();
      }
      throw muleException;
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
      BaseEventContext childContext = newChildContext(event, empty());
      return just(event)
          .transform(publisher -> from(publisher).flatMap(request -> {
            return from(internalProcessWithChildContext(request, processor, childContext, false,
                                                        childContext.getResponsePublisher()));
          }))
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
    BaseEventContext childContext = newChildContext(event, componentLocation);
    return internalProcessWithChildContext(event, processor, childContext, true, childContext.getResponsePublisher());
  }

  /**
   * Creates a new {@link BaseEventContext} which is child of the one in the given {@code event}
   *
   * @param event the parent event
   * @param componentLocation the location of the component creating the child context
   * @return a child {@link BaseEventContext}
   */
  public static BaseEventContext newChildContext(CoreEvent event, Optional<ComponentLocation> componentLocation) {
    return child(((BaseEventContext) event.getContext()), componentLocation);
  }

  public static Publisher<CoreEvent> processWithChildContext(CoreEvent event, ReactiveProcessor processor,
                                                             BaseEventContext childContext) {
    return internalProcessWithChildContext(event, processor, childContext, true, childContext.getResponsePublisher());
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
    BaseEventContext childContext = child(((BaseEventContext) event.getContext()), componentLocation, exceptionHandler);
    return internalProcessWithChildContext(event, processor, childContext, true, childContext.getResponsePublisher());
  }

  private static Publisher<CoreEvent> internalProcessWithChildContext(CoreEvent event, ReactiveProcessor processor,
                                                                      BaseEventContext child, boolean completeParentOnEmpty,
                                                                      Publisher<CoreEvent> responsePublisher) {
    return just(quickCopy(child, event))
        .transform(processor)
        .doOnNext(completeSuccessIfNeeded(child, true))
        .switchIfEmpty(from(responsePublisher))
        .map(result -> quickCopy(child.getParentContext().get(), result))
        .doOnError(MessagingException.class,
                   me -> me.setProcessedEvent(quickCopy(child.getParentContext().get(), me.getEvent())))
        .doOnSuccess(result -> {
          if (result == null && completeParentOnEmpty) {
            child.getParentContext().get().success();
          }
        });
  }

  public static Consumer<CoreEvent> completeSuccessIfNeeded(EventContext child, boolean complete) {
    return result -> {
      if (!((BaseEventContext) child).isComplete() && complete) {
        ((BaseEventContext) child).success(result);
      }
    };
  }

  public static Consumer<Throwable> completeErrorIfNeeded(EventContext child, boolean complete) {
    return throwable -> {
      if (!((BaseEventContext) child).isComplete() && complete) {
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

  /**
   * Transform a given {@link Publisher} using a {@link ReactiveProcessor}. Primarily for use in the implementation of
   * {@link ReactiveProcessor} in other class-loaders.
   *
   * @param publisher the publisher to transform
   * @param processor the processor to transform publisher with
   * @return the transformed publisher
   * @since 4.1
   */
  public static Publisher<CoreEvent> transform(Publisher<CoreEvent> publisher, ReactiveProcessor processor) {
    return Flux.from(publisher).transform(processor);
  }

  /**
   * Transform a given {@link Publisher} using a mapper function. Primarily for use in the implementation of mapping in other
   * class-loaders.
   *
   * @param publisher the publisher to transform
   * @param mapper the mapper to map publisher items with
   * @return the transformed publisher
   * @since 4.2
   */
  public static Publisher<CoreEvent> map(Publisher<CoreEvent> publisher, Function<CoreEvent, CoreEvent> mapper) {
    return Flux.from(publisher).map(mapper);
  }

  /**
   * Perform processing using the provided {@link Function} for each {@link CoreEvent}. Primarily for use in the implementation of
   * {@link ReactiveProcessor} in other class-loaders.
   *
   * @param publisher the publisher to transform
   * @param function the function to apply to each event.
   * @param component the component that implements this functionality.
   * @return the transformed publisher
   * @since 4.1
   */
  public static Publisher<CoreEvent> flatMap(Publisher<CoreEvent> publisher,
                                             Function<CoreEvent, Publisher<CoreEvent>> function, Component component) {
    return Flux.from(publisher)
        .flatMap(event -> from(function.apply(event))
            .onErrorMap(e -> !(e instanceof MessagingException), e -> new MessagingException(event, e, component)));
  }

  /**
   * Creates a new {@link Publisher} that will emit the given {@code event}, publishing it on the given {@code executor}.
   *
   * @param event the {@link CoreEvent} to emit
   * @param executor the thread pool where the event will be published.
   * @return the created publisher
   * @since 4.2
   */
  public static Publisher<CoreEvent> justPublishOn(CoreEvent event, ExecutorService executor) {
    return Flux.just(event).publishOn(fromExecutorService(executor));
  }
}

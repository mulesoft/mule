/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.core.api.event.BaseEvent.builder;
import static org.mule.runtime.core.api.rx.Exceptions.rxExceptionToMuleException;
import static org.mule.runtime.core.internal.event.DefaultEventContext.child;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.runtime.core.api.event.BaseEventContext;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder;

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
   * a {@link BaseEvent} result is available or throws an exception in the case of an error. This is currently used widely to
   * continue to support the blocking {@link Processor#process(BaseEvent)} API when the implementation of a {@link Processor}
   * is implemented via {@link ReactiveProcessor#apply(Object)}.
   * <p>
   * The {@link BaseEvent} returned by this method will <b>not</b> be completed with a {@link BaseEvent} or
   * {@link Throwable} unless the delegate {@link Processor} does this.
   *
   * TODO MULE-13054 Remove blocking processor API
   *
   * @param event event to process
   * @param processor processor to adapt
   * @return result event
   * @throws MuleException if an error occurs
   */
  public static BaseEvent processToApply(BaseEvent event, ReactiveProcessor processor) throws MuleException {
    return processToApply(event, processor, false);
  }

  /**
   * Adapt a {@link ReactiveProcessor} that implements {@link ReactiveProcessor#apply(Object)} to a blocking API that blocks until
   * a {@link BaseEvent} result is available or throws an exception in the case of an error. This is currently used widely to
   * continue to support the blocking {@link Processor#process(BaseEvent)} API when the implementation of a {@link Processor}
   * is implemented via {@link ReactiveProcessor#apply(Object)}.
   * <p>
   * The {@link BaseEvent} returned by this method will be completed with a {@link BaseEvent} or {@link Throwable} if
   * {@code completeContext} is {@code true}.
   *
   * TODO MULE-13054 Remove blocking processor API
   *
   * @param event event to process
   * @param processor processor to adapt
   * @return result event
   * @throws MuleException if an error occurs
   */
  public static BaseEvent processToApply(BaseEvent event, ReactiveProcessor processor, boolean completeContext)
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
   * a {@link BaseEvent} result is available or throws an exception in the case of an error. This method differs from
   * {@link #processToApply(BaseEvent, ReactiveProcessor)} in that processing will be scoped using a child
   * {@link EventContext} such that completion of the {@link EventContext} in the delegate {@link Processor} does not complete the
   * original {@link EventContext}.
   *
   * @param event event to process
   * @param processor processor to adapt
   * @return result event
   * @throws MuleException if an error occurs
   */
  public static BaseEvent processToApplyWithChildContext(BaseEvent event, ReactiveProcessor processor)
      throws MuleException {
    try {
      return just(event)
          .transform(publisher -> from(publisher).then(request -> Mono
              .from(internalProcessWithChildContext(request, processor,
                                                    child(((BaseEventContext) event.getContext()), empty()), false))))
          .block();
    } catch (Throwable e) {
      throw rxExceptionToMuleException(e);
    }
  }

  /**
   * Process an {@link BaseEvent} with a given {@link ReactiveProcessor} returning a {@link Publisher<InternalEvent>} via
   * which the future {@link BaseEvent} or {@link Throwable} will be published.
   * <p/>
   * The {@link BaseEvent} returned by this method <b>will</b> be completed with the same {@link BaseEvent} instance and
   * if an error occurs during processing the {@link EventContext} will be completed with the error.
   *
   * @param event event to process
   * @param processor processor to use
   * @return future result
   */
  public static Publisher<BaseEvent> process(BaseEvent event, ReactiveProcessor processor) {
    return just(event).transform(processor)
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
  public static Publisher<BaseEvent> processWithChildContext(BaseEvent event, ReactiveProcessor processor,
                                                             Optional<ComponentLocation> componentLocation) {
    return internalProcessWithChildContext(event, processor,
                                           child(((BaseEventContext) event.getContext()), componentLocation), true);
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
   * @param exceptionHandler used to handle {@link MessagingException}'s.
   * @return the future result of processing processor.
   */
  public static Publisher<BaseEvent> processWithChildContext(BaseEvent event, ReactiveProcessor processor,
                                                             Optional<ComponentLocation> componentLocation,
                                                             MessagingExceptionHandler exceptionHandler) {
    return internalProcessWithChildContext(event, processor,
                                           child(((BaseEventContext) event.getContext()), componentLocation,
                                                 exceptionHandler),
                                           true);
  }

  private static Publisher<BaseEvent> internalProcessWithChildContext(BaseEvent event, ReactiveProcessor processor,
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

  private static Consumer<BaseEvent> completeSuccessIfNeeded(EventContext child, boolean complete) {
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
   * @param muleContext the context
   * @param rootContainerName the component root container element
   * @return the processing strategy of the root component if it was an instance of {@link FlowConstruct}, empty otherwise.
   */
  public static Optional<ProcessingStrategy> getProcessingStrategy(MuleContext muleContext, String rootContainerName) {
    Optional<ProcessingStrategy> processingStrategy = empty();
    Object object = muleContext.getRegistry().get(rootContainerName);
    if (object instanceof FlowConstruct) {
      processingStrategy = of(muleContext.getRegistry().lookupFlowConstruct(rootContainerName).getProcessingStrategy());
    }
    return processingStrategy;
  }

}

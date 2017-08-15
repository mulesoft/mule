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
import static org.mule.runtime.core.DefaultEventContext.child;
import static org.mule.runtime.core.api.InternalEvent.builder;
import static org.mule.runtime.core.api.rx.Exceptions.rxExceptionToMuleException;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.InternalEventContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder;

import org.reactivestreams.Publisher;

import java.util.List;
import java.util.Optional;

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
   * Adapt a {@link ReactiveProcessor} used via non-blocking API {@link ReactiveProcessor#apply(Object)} by blocking and waiting
   * for response {@link InternalEvent} or throwing an {@link MuleException} in the case of an error.
   * <p/>
   * If the {@link ReactiveProcessor} drops the event due to an error or stop action, then the result of returned will be that of
   * the {@link InternalEventContext} completion. Attempting to adapt a processor implementation that filters events and does not
   * complete the {@link InternalEventContext} will cause this method to never return.
   *
   * TODO MULE-13054 Remove blocking processor API
   *
   * @param event event to process.
   * @param processor processor to adapt.
   * @return result event
   * @throws MuleException
   */
  public static InternalEvent processToApply(InternalEvent event, ReactiveProcessor processor) throws MuleException {
    try {
      return just(event).transform(processor).switchIfEmpty(from(event.getContext().getResponsePublisher())).block();
    } catch (Throwable e) {
      throw rxExceptionToMuleException(e);
    }
  }

  /**
   * Adapt a {@link ReactiveProcessor} used via non-blocking API {@link ReactiveProcessor#apply(Object)} by blocking and waiting
   * for response {@link InternalEvent} or throwing an {@link MuleException} in the case of an error.
   * <p/>
   * If the {@link ReactiveProcessor} drops the event due to an error or stop action, then the result of returned will be that of
   * the {@link InternalEventContext} completion. Attempting to adapt a processor implementation that filters events and does not
   * complete the {@link InternalEventContext} will cause this method to never return.
   * 
   * This method uses a child context so that if processing occurs using non-blocking further down the chain this will not
   * interfere with current context.
   *
   * TODO MULE-13054 Remove blocking processor API
   *
   * @param event event to process.
   * @param processor processor to adapt.
   * @return result event
   * @throws MuleException
   */
  public static InternalEvent processToApplyWithChildContext(InternalEvent event, ReactiveProcessor processor)
      throws MuleException {
    try {
      return just(event)
          .transform(publisher -> from(publisher).then(request -> Mono
              .from(internalProcessWithChildContext(request, processor, child(event.getContext(), empty()), false))))
          .block();
    } catch (Throwable e) {
      throw rxExceptionToMuleException(e);
    }
  }

  /**
   * Process a {@link ReactiveProcessor} using a child {@link InternalEventContext}. This is useful if it is necessary to perform
   * processing in a scope and handle an empty result or error locally rather than complete the response for the whole Flow.
   * <p>
   * No error-handling will be performed when errors occur.
   *
   * @param event the event to process.
   * @param processor the processor to process.
   * @param componentLocation
   * @return the future result of processing processor.
   */
  public static Publisher<InternalEvent> processWithChildContext(InternalEvent event, ReactiveProcessor processor,
                                                                 Optional<ComponentLocation> componentLocation) {
    return internalProcessWithChildContext(event, processor, child(event.getContext(), componentLocation), true);
  }

  /**
   * Process a {@link ReactiveProcessor} using a child {@link InternalEventContext}. This is useful if it is necessary to perform
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
  public static Publisher<InternalEvent> processWithChildContext(InternalEvent event, ReactiveProcessor processor,
                                                                 Optional<ComponentLocation> componentLocation,
                                                                 MessagingExceptionHandler exceptionHandler) {
    return internalProcessWithChildContext(event, processor,
                                           child(event.getContext(), componentLocation, exceptionHandler),
                                           true);
  }

  private static Publisher<InternalEvent> internalProcessWithChildContext(InternalEvent event, ReactiveProcessor processor,
                                                                          InternalEventContext child,
                                                                          boolean completeParentOnEmpty) {
    return just(builder(child, event).build())
        .transform(processor)
        .doOnNext(result -> {
          if (!(from(child.getResponsePublisher()).toFuture().isDone())) {
            child.success(result);
          }
        })
        .switchIfEmpty(from(child.getResponsePublisher()))
        .map(result -> builder(event.getContext(), result).build())
        .doOnError(MessagingException.class,
                   me -> me.setProcessedEvent(builder(event.getContext(), me.getEvent()).build()))
        .doOnSuccess(result -> {
          if (result == null && completeParentOnEmpty) {
            event.getContext().success();
          }
        });
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

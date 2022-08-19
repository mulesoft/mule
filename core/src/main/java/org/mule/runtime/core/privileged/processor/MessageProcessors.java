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
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.FLOW;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.ROUTER;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SCOPE;
import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.core.api.rx.Exceptions.rxExceptionToMuleException;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static org.mule.runtime.core.internal.event.DefaultEventContext.child;
import static org.mule.runtime.core.internal.event.EventQuickCopy.quickCopy;
import static org.mule.runtime.core.internal.util.rx.RxUtils.subscribeFluxOnPublisherSubscription;
import static org.mule.runtime.core.privileged.profiling.tracing.ChildSpanCustomizationInfo.getDefaultChildSpanInfo;

import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;
import static reactor.core.publisher.Mono.subscriberContext;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.notification.FlowStackElement;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategySupplier;
import org.mule.runtime.core.internal.context.notification.DefaultFlowCallStack;
import org.mule.runtime.core.internal.event.EventContextDeepNestingException;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.processor.strategy.TransactionAwareProactorStreamEmitterProcessingStrategyFactory;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.mule.runtime.core.internal.rx.FluxSinkRecorderToReactorSinkAdapter;
import org.mule.runtime.core.internal.rx.MonoSinkRecorder;
import org.mule.runtime.core.internal.rx.MonoSinkRecorderToReactorSinkAdapter;
import org.mule.runtime.core.internal.rx.SinkRecorderToReactorSinkAdapter;
import org.mule.runtime.core.internal.util.rx.RxUtils;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.privileged.profiling.tracing.ChildSpanCustomizationInfo;
import org.mule.runtime.core.privileged.profiling.tracing.SpanCustomizationInfo;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Some convenience methods for message processors.
 */
public class MessageProcessors {

  public static final String WITHIN_PROCESS_TO_APPLY = "messageProcessors.withinProcessToApply";
  protected static final String WITHIN_PROCESS_WITH_CHILD_CONTEXT = "messageProcessors.withinProcessWithChildContext";
  private static final Logger LOGGER = LoggerFactory.getLogger(MessageProcessors.class);

  private MessageProcessors() {
    // do not instantiate
  }

  /**
   * Creates a new {@link MessageProcessorChain} from a {@link List} of {@link Processor}'s. Note that this performs chains
   * construction but will not inject {@link MuleContext} or perform any lifecycle.
   *
   * @param processingStrategy the processing strategy to use for configuring the chain. It may be {@link Optional#empty()}.
   * @param processors         list of processors to construct chains from.
   * @return new {@link MessageProcessorChain} instance.
   */
  public static MessageProcessorChain newChain(Optional<ProcessingStrategy> processingStrategy, List<Processor> processors) {
    if (processors.size() == 1 && processors.get(0) instanceof MessageProcessorChain) {
      return (MessageProcessorChain) processors.get(0);
    } else {
      return buildNewChainWithListOfProcessors(processingStrategy, processors);
    }
  }

  public static MessageProcessorChain newChain(Optional<ProcessingStrategy> processingStrategy, List<Processor> processors,
                                               String name) {
    if (processors.size() == 1 && processors.get(0) instanceof MessageProcessorChain) {
      return (MessageProcessorChain) processors.get(0);
    } else {
      return buildNewChainWithListOfProcessors(processingStrategy, processors, new SpanCustomizationInfo() {

        @Override
        public String getName(CoreEvent coreEvent) {
          return name;
        }

        @Override
        public ChildSpanCustomizationInfo getChildSpanCustomizationInfo() {
          return getDefaultChildSpanInfo();
        }
      });
    }
  }

  public static MessageProcessorChain buildNewChainWithListOfProcessors(Optional<ProcessingStrategy> processingStrategy,
                                                                        List<Processor> processors) {
    DefaultMessageProcessorChainBuilder defaultMessageProcessorChainBuilder = new DefaultMessageProcessorChainBuilder();
    processingStrategy.ifPresent(defaultMessageProcessorChainBuilder::setProcessingStrategy);
    return defaultMessageProcessorChainBuilder.chain(processors).build();
  }

  public static MessageProcessorChain buildNewChainWithListOfProcessors(Optional<ProcessingStrategy> processingStrategy,
                                                                        List<Processor> processors,
                                                                        FlowExceptionHandler messagingExceptionHandler) {
    return buildNewChainWithListOfProcessors(processingStrategy, processors, messagingExceptionHandler, (String) null);
  }

  public static MessageProcessorChain buildNewChainWithListOfProcessors(Optional<ProcessingStrategy> processingStrategy,
                                                                        List<Processor> processors,
                                                                        FlowExceptionHandler messagingExceptionHandler,
                                                                        String name) {
    DefaultMessageProcessorChainBuilder defaultMessageProcessorChainBuilder = new DefaultMessageProcessorChainBuilder();
    processingStrategy.ifPresent(defaultMessageProcessorChainBuilder::setProcessingStrategy);
    defaultMessageProcessorChainBuilder.setMessagingExceptionHandler(messagingExceptionHandler);
    defaultMessageProcessorChainBuilder.setName(name);
    return defaultMessageProcessorChainBuilder.chain(processors).build();
  }

  public static MessageProcessorChain buildNewChainWithListOfProcessors(Optional<ProcessingStrategy> processingStrategy,
                                                                        List<Processor> processors,
                                                                        SpanCustomizationInfo spanCustomizationInfo) {
    DefaultMessageProcessorChainBuilder defaultMessageProcessorChainBuilder = new DefaultMessageProcessorChainBuilder();
    processingStrategy.ifPresent(defaultMessageProcessorChainBuilder::setProcessingStrategy);
    defaultMessageProcessorChainBuilder.setSpanCustomizationInfo(spanCustomizationInfo);
    return defaultMessageProcessorChainBuilder.chain(processors).build();
  }

  public static MessageProcessorChain buildNewChainWithListOfProcessors(Optional<ProcessingStrategy> processingStrategy,
                                                                        List<Processor> processors,
                                                                        FlowExceptionHandler messagingExceptionHandler,
                                                                        SpanCustomizationInfo spanCustomizationInfo) {
    DefaultMessageProcessorChainBuilder defaultMessageProcessorChainBuilder = new DefaultMessageProcessorChainBuilder();
    processingStrategy.ifPresent(defaultMessageProcessorChainBuilder::setProcessingStrategy);
    defaultMessageProcessorChainBuilder.setMessagingExceptionHandler(messagingExceptionHandler);
    defaultMessageProcessorChainBuilder.setSpanCustomizationInfo(spanCustomizationInfo);
    return defaultMessageProcessorChainBuilder.chain(processors).build();
  }

  /**
   * Creates a new {@link MessageProcessorChain} from a {@link List} of {@link Processor}'s. Note that this performs chains
   * construction but will not inject {@link MuleContext} or perform any lifecycle.
   *
   * @param processingStrategy the processing strategy to use for configuring the chain. It may be {@link Optional#empty()}.
   * @param processors         list of processors to construct chains from.
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
   * @param event     event to process
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
   * @param event     event to process
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
   * @param event     event to process
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
          .subscriberContext(ctx -> ctx.put(WITHIN_PROCESS_TO_APPLY, true))
          .block();
    } catch (Throwable e) {
      if (e.getCause() instanceof InterruptedException) {
        currentThread().interrupt();
      }
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
   * @param event     event to process
   * @param processor processor to adapt
   * @return result event
   * @throws MuleException if an error occurs
   */
  public static CoreEvent processToApplyWithChildContext(CoreEvent event, ReactiveProcessor processor)
      throws MuleException {
    try {
      return just(event)
          .transform(publisher -> from(publisher)
              .flatMap(request -> from(internalProcessWithChildContext(quickCopy(newChildContext(event, empty()), event),
                                                                       processor, false, true))))
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
   * @param event     event to process
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
        .doOnError(completeErrorIfNeeded((event.getContext()), true))
        .subscriberContext(ctx -> ctx.put(WITHIN_PROCESS_TO_APPLY, true));
  }

  /**
   * Process a {@link ReactiveProcessor} using a child {@link BaseEventContext}. This is useful if it is necessary to perform
   * processing in a scope and handle an empty result or error locally rather than complete the response for the whole Flow.
   * <p>
   * When using this method, the usage of {@link Mono#onErrorContinue(java.util.function.BiConsumer)} methods must be avoided,
   * since the returned publisher already configures its error handling.
   *
   * @param event             the event to process.
   * @param processor         the processor to process.
   * @param componentLocation
   * @return the future result of processing processor.
   */
  public static Publisher<CoreEvent> processWithChildContext(CoreEvent event, ReactiveProcessor processor,
                                                             Optional<ComponentLocation> componentLocation) {
    BaseEventContext childContext = newChildContext(event, componentLocation);
    return internalProcessWithChildContextAlwaysComplete(event, quickCopy(childContext, event), processor, true);
  }

  /**
   * Process a {@link ReactiveProcessor} using a child {@link BaseEventContext}. This is useful if it is necessary to perform
   * processing in a scope and handle an empty result or error locally rather than complete the response for the whole Flow.
   * <p>
   * After executing the provided processor, the {@link EventContext} of the given event will be completed.
   * <p>
   * When using this method, the usage of {@link Mono#onErrorContinue(java.util.function.BiConsumer)} methods must be avoided,
   * since the returned publisher already configures its error handling.
   *
   * @param event             the event to process.
   * @param processor         the processor to process.
   * @param componentLocation
   * @return the future result of processing processor.
   */
  public static Publisher<CoreEvent> processWithChildContextDontComplete(CoreEvent event, ReactiveProcessor processor,
                                                                         Optional<ComponentLocation> componentLocation) {
    BaseEventContext childContext = newChildContext(event, componentLocation);
    return internalProcessWithChildContext(quickCopy(childContext, event), processor, true, true);
  }

  /**
   * Creates a new {@link BaseEventContext} which is child of the one in the given {@code event}
   * <p>
   * When using this method, the usage of {@link Mono#onErrorContinue(java.util.function.BiConsumer)} methods must be avoided,
   * since the returned publisher already configures its error handling.
   *
   * @param event             the parent event
   * @param componentLocation the location of the component creating the child context
   * @return a child {@link BaseEventContext}
   */
  public static BaseEventContext newChildContext(CoreEvent event, Optional<ComponentLocation> componentLocation) {
    return child(((BaseEventContext) event.getContext()), componentLocation);
  }

  public static Publisher<CoreEvent> processWithChildContext(CoreEvent event, ReactiveProcessor processor,
                                                             BaseEventContext childContext) {
    return internalProcessWithChildContext(quickCopy(childContext, event), processor, true, true);
  }

  /**
   * Process a {@link ReactiveProcessor} using a child {@link EventContext}. This is useful if it is necessary to perform
   * processing in a scope and handle an empty result or error locally rather than complete the response for the whole Flow.
   * <p>
   * The {@link FlowExceptionHandler} configured on {@link MessageProcessorChain} or {@link FlowConstruct} will be used to handle
   * any errors that occur.
   * <p>
   * When using this method, the usage of {@link Mono#onErrorContinue(java.util.function.BiConsumer)} methods must be avoided,
   * since the returned publisher already configures its error handling.
   *
   * @param event             the event to process.
   * @param processor         the processor to process.
   * @param componentLocation
   * @param exceptionHandler  used to handle {@link Exception}'s.
   * @return the future result of processing processor.
   *
   * @deprecated Since 4.3.0, use {@link #processWithChildContext(CoreEvent, ReactiveProcessor, Optional)} instead and rely on the
   *             provided {@code processor} to do the error handling.
   */
  @Deprecated
  public static Publisher<CoreEvent> processWithChildContext(CoreEvent event, ReactiveProcessor processor,
                                                             Optional<ComponentLocation> componentLocation,
                                                             FlowExceptionHandler exceptionHandler) {
    BaseEventContext childContext = child(((BaseEventContext) event.getContext()), componentLocation, exceptionHandler);
    return internalProcessWithChildContextAlwaysComplete(event, quickCopy(childContext, event), processor, true);
  }

  /**
   * Process a {@link ReactiveProcessor} using a child {@link EventContext}. This is useful if it is necessary to perform
   * processing in a scope and handle an empty result or error locally rather than complete the response for the whole Flow.
   * <p>
   * After executing the provided processor, the {@link EventContext} of the given event will be completed.
   * <p>
   * The {@link FlowExceptionHandler} configured on {@link MessageProcessorChain} or {@link FlowConstruct} will be used to handle
   * any errors that occur.
   * <p>
   * When using this method, the usage of {@link Mono#onErrorContinue(java.util.function.BiConsumer)} methods must be avoided,
   * since the returned publisher already configures its error handling.
   *
   * @param event             the event to process.
   * @param processor         the processor to process.
   * @param componentLocation
   * @param exceptionHandler  used to handle {@link Exception}'s.
   * @return the future result of processing processor.
   *
   * @deprecated Since 4.3.0, use {@link #processWithChildContextDontComplete(CoreEvent, ReactiveProcessor, Optional)} instead and
   *             rely on the provided {@code processor} to do the error handling.
   */
  @Deprecated
  public static Publisher<CoreEvent> processWithChildContextDontComplete(CoreEvent event, ReactiveProcessor processor,
                                                                         Optional<ComponentLocation> componentLocation,
                                                                         FlowExceptionHandler exceptionHandler) {
    BaseEventContext childContext = child(((BaseEventContext) event.getContext()), componentLocation, exceptionHandler);
    return internalProcessWithChildContext(quickCopy(childContext, event), processor, true, true);
  }

  /**
   * Process a {@link Processor} using a child {@link EventContext}. This is useful if it is necessary to perform processing in a
   * scope and handle an empty result or error locally rather than complete the response for the whole Flow.
   *
   * @param event             the event to process.
   * @param processor         the processor to process.
   * @param componentLocation
   * @return the result of processing processor.
   * @throws MuleException
   */
  public static CoreEvent processWithChildContextBlocking(CoreEvent event, Processor processor,
                                                          Optional<ComponentLocation> componentLocation)
      throws MuleException {
    return internalProcessWithChildContextBlocking(event, processor,
                                                   child(((BaseEventContext) event.getContext()), componentLocation));
  }


  /**
   * Process a {@link Processor} using a child {@link EventContext}. This is useful if it is necessary to perform processing in a
   * scope and handle an empty result or error locally rather than complete the response for the whole Flow.
   * <p>
   * The {@link FlowExceptionHandler} configured on {@link MessageProcessorChain} or {@link FlowConstruct} will be used to handle
   * any errors that occur.
   *
   * @param event             the event to process.
   * @param processor         the processor to process.
   * @param componentLocation
   * @param exceptionHandler  used to handle {@link Exception}'s.
   * @return the result of processing processor.
   * @throws MuleException
   *
   * @deprecated Since 4.3.0, use {@link #processWithChildContextBlocking(CoreEvent, Processor, Optional)} instead and rely on the
   *             provided {@code processor} to do the error handling.
   */
  @Deprecated
  public static CoreEvent processWithChildContextBlocking(CoreEvent event, Processor processor,
                                                          Optional<ComponentLocation> componentLocation,
                                                          FlowExceptionHandler exceptionHandler)
      throws MuleException {
    return internalProcessWithChildContextBlocking(event, processor,
                                                   child(((BaseEventContext) event.getContext()), componentLocation,
                                                         exceptionHandler));
  }

  private static CoreEvent internalProcessWithChildContextBlocking(CoreEvent event, Processor processor, BaseEventContext child)
      throws MuleException {
    final Publisher<CoreEvent> childResponsePublisher = child.getResponsePublisher();

    CoreEvent result;
    try {
      result = processor.process(quickCopy(child, event));
      completeSuccessIfNeeded().accept(result);
    } catch (MuleException e) {
      try {
        result = Mono.from(childResponsePublisher).block();
      } catch (Throwable t) {
        Throwable throwable = unwrap(t);
        if (throwable instanceof MessagingException) {
          final MessagingException error = (MessagingException) throwable;
          throw rxExceptionToMuleException(new MessagingException(toParentContext(error.getEvent()), error));
        }
        if (t.getCause() instanceof InterruptedException) {
          currentThread().interrupt();
        }
        throw rxExceptionToMuleException(t);
      }
    }

    return quickCopy(event.getContext(), result);
  }

  private static Publisher<CoreEvent> internalProcessWithChildContext(CoreEvent eventChildCtx,
                                                                      ReactiveProcessor processor,
                                                                      boolean completeParentIfEmpty, boolean propagateErrors) {
    MonoSinkRecorder<Either<MessagingException, CoreEvent>> errorSwitchSinkSinkRef = new MonoSinkRecorder<>();

    return Mono.<CoreEvent>create(sink -> {
      childContextResponseHandler(eventChildCtx, new MonoSinkRecorderToReactorSinkAdapter<>(errorSwitchSinkSinkRef),
                                  completeParentIfEmpty, propagateErrors);

      sink.success(eventChildCtx);
    })
        .toProcessor()
        .transform(processor)
        .doOnNext(completeSuccessIfNeeded())
        .switchIfEmpty(Mono.<Either<MessagingException, CoreEvent>>create(errorSwitchSinkSinkRef)
            .map(RxUtils.<MessagingException>propagateErrorResponseMapper())
            .toProcessor())
        .map(MessageProcessors::toParentContext)
        .subscriberContext(ctx -> ctx.put(WITHIN_PROCESS_WITH_CHILD_CONTEXT, true)
            .put(WITHIN_PROCESS_TO_APPLY, true));
  }

  private static Publisher<CoreEvent> internalProcessWithChildContextAlwaysComplete(CoreEvent event,
                                                                                    CoreEvent eventChildCtx,
                                                                                    ReactiveProcessor processor,
                                                                                    boolean completeParentIfEmpty) {
    return Mono.from(internalProcessWithChildContext(eventChildCtx, processor, completeParentIfEmpty, true))
        .doOnSuccess(result -> {
          if (result == null) {
            ((BaseEventContext) event.getContext()).success();
          }
        })
        .doOnError(e -> {
          ((BaseEventContext) event.getContext()).error(e);
        })
        .onErrorMap(MessagingException.class,
                    me -> new MessagingException(quickCopy(eventChildCtx.getContext(), me.getEvent()), me));
  }

  private static Publisher<CoreEvent> internalApplyWithChildContext(Publisher<CoreEvent> eventChildCtxPub,
                                                                    ReactiveProcessor processor,
                                                                    boolean completeParentIfEmpty, boolean propagateErrors) {
    return Flux.from(eventChildCtxPub)
        .compose(eventPub -> subscriberContext()
            .flatMapMany(ctx -> {
              if (ctx.getOrDefault(WITHIN_PROCESS_WITH_CHILD_CONTEXT, false)
                  || ctx.getOrDefault(WITHIN_PROCESS_TO_APPLY, false)) {
                // This is a workaround for https://github.com/reactor/reactor-core/issues/1705
                // If this processor is already wrapped in a Mono, there is no gain in using a Flux, and this way the issue
                // mentioned above is avoided.

                return eventPub
                    .flatMap(event -> internalProcessWithChildContext(event, processor, completeParentIfEmpty, propagateErrors));
              } else {
                FluxSinkRecorder<Either<MessagingException, CoreEvent>> errorSwitchSinkSinkRef = new FluxSinkRecorder<>();
                final FluxSinkRecorderToReactorSinkAdapter<Either<MessagingException, CoreEvent>> errorSwitchSinkSinkRefAdapter =
                    new FluxSinkRecorderToReactorSinkAdapter<>(errorSwitchSinkSinkRef);

                final Flux<Either<MessagingException, CoreEvent>> upstream = Flux.from(eventChildCtxPub)
                    .doOnNext(eventChildCtx -> childContextResponseHandler(eventChildCtx, errorSwitchSinkSinkRefAdapter,
                                                                           completeParentIfEmpty, propagateErrors))
                    .transform(processor)
                    // This Either here is used to propagate errors. If the error is sent directly through the merged with Flux,
                    // it will be cancelled, ignoring the onErrorContinue of the parent Flux.
                    .map(event -> right(MessagingException.class, event));

                return subscribeFluxOnPublisherSubscription(errorSwitchSinkSinkRef.flux(), upstream,
                                                            completeSuccessEitherIfNeeded(),
                                                            errorSwitchSinkSinkRef::error,
                                                            errorSwitchSinkSinkRef::complete)
                                                                .map(RxUtils.<MessagingException>propagateErrorResponseMapper()
                                                                    .andThen(MessageProcessors::toParentContext));
              }
            }));
  }

  private static void childContextResponseHandler(CoreEvent eventChildCtx,
                                                  SinkRecorderToReactorSinkAdapter<Either<MessagingException, CoreEvent>> errorSwitchSinkSinkRef,
                                                  boolean completeParentIfEmpty, boolean propagateErrors) {
    ((BaseEventContext) eventChildCtx.getContext()).onResponse((response, throwable) -> {
      try {
        if (throwable != null) {
          MessagingException error = (MessagingException) throwable;

          final CoreEvent parentContextEvent = toParentContext(error.getEvent());
          if (propagateErrors || error.getCause() instanceof EventContextDeepNestingException) {
            error = new MessagingException(parentContextEvent, error);
          } else {
            final FlowStackElement currentStackEntry = ((DefaultFlowCallStack) eventChildCtx.getFlowCallStack()).peek();
            if (currentStackEntry != null) {
              // For flow-ref to flows, avoid creating a second MessagingException, and instead mutate the thrown on so it has the
              // proper state when is bubbled.
              DefaultFlowCallStack parentCallStack = (DefaultFlowCallStack) parentContextEvent.getFlowCallStack();
              List<FlowStackElement> childFlowStacks = eventChildCtx.getFlowCallStack().getElements();
              ArrayDeque<FlowStackElement> remaining = new ArrayDeque<>();

              // MULE-18883: In order to make parentCallStack consistent with the execution stack, we need to add (in the proper
              // order) all the remaining FlowStackElement:
              // 1. save all the elements in the child flow stack until we reach the same element located on the top of the
              // parent stack.
              for (FlowStackElement e : childFlowStacks) {
                if (e.equals(parentCallStack.peek())) {
                  break;
                }
                remaining.push(e);
              }

              // 2. add the collected elements from previous step in the parent call stack
              while (!remaining.isEmpty()) {
                parentCallStack.push(remaining.pop());
              }
            }
            error.setProcessedEvent(parentContextEvent);
          }
          errorSwitchSinkSinkRef.next(left(error));
        } else if (response == null && completeParentIfEmpty) {
          getParentContext(eventChildCtx).success();
          errorSwitchSinkSinkRef.next();
        } else if (response == null) {
          errorSwitchSinkSinkRef.next();
        } else {
          errorSwitchSinkSinkRef.next(right(response));
        }
      } catch (Exception e) {
        LOGGER.error("Uncaught exception in childContextResponseHandler", e);
      }
    });
  }

  private static CoreEvent toParentContext(final CoreEvent event) {
    return quickCopy(getParentContext(event), event);
  }

  private static BaseEventContext getParentContext(final CoreEvent event) {
    return ((BaseEventContext) event.getContext()).getParentContext().orElse(null);
  }

  /**
   * Process a {@link ReactiveProcessor} using a child {@link BaseEventContext}. This is useful if it is necessary to perform
   * processing in a scope and handle an empty result or error locally rather than complete the response for the whole Flow.
   *
   * @param eventPub          the publisher for the event(s) to process.
   * @param processor         the processor to process.
   * @param componentLocation
   * @return the future result of processing processor.
   */
  public static Publisher<CoreEvent> applyWithChildContext(Publisher<CoreEvent> eventPub, ReactiveProcessor processor,
                                                           Optional<ComponentLocation> componentLocation) {
    return internalApplyWithChildContext(Flux.from(eventPub)
        .map(event -> quickCopy(child(((BaseEventContext) event.getContext()), componentLocation), event)),
                                         processor, true, true);
  }

  /**
   * Process a {@link ReactiveProcessor} using a child {@link BaseEventContext}. This is useful if it is necessary to perform
   * processing in a scope and handle an empty result or error locally rather than complete the response for the whole Flow.
   * <p>
   * This implementation will do a limited error handling because it is assumed that the bulk of the error handling will be done
   * somewhere else (for instance, on a {@code flow-ref} to a {@code flow}, the error bubbling is done by the flow itself, so the
   * child context needs not do that).
   *
   * @param eventPub          the publisher for the event(s) to process.
   * @param processor         the processor to process.
   * @param componentLocation
   * @return the future result of processing processor.
   */
  public static Publisher<CoreEvent> applyWithChildContextDontPropagateErrors(Publisher<CoreEvent> eventPub,
                                                                              ReactiveProcessor processor,
                                                                              Optional<ComponentLocation> componentLocation) {
    return internalApplyWithChildContext(Flux.from(eventPub)
        .map(event -> quickCopy(child(((BaseEventContext) event.getContext()), componentLocation), event)),
                                         processor, true, false);
  }

  /**
   * Process a {@link ReactiveProcessor} using a child {@link EventContext}. This is useful if it is necessary to perform
   * processing in a scope and handle an empty result or error locally rather than complete the response for the whole Flow.
   * <p>
   * The {@link FlowExceptionHandler} configured on {@link MessageProcessorChain} or {@link FlowConstruct} will be used to handle
   * any errors that occur.
   *
   * @param eventPub          the publisher for the event(s) to process.
   * @param processor         the processor to process.
   * @param componentLocation
   * @param exceptionHandler  used to handle {@link Exception}'s.
   * @return the future result of processing processor.
   *
   * @deprecated Since 4.3.0, use {@link #applyWithChildContext(Publisher, ReactiveProcessor, Optional)} instead and rely on the
   *             provided {@code processor} to do the error handling.
   */
  @Deprecated
  public static Publisher<CoreEvent> applyWithChildContext(Publisher<CoreEvent> eventPub, ReactiveProcessor processor,
                                                           Optional<ComponentLocation> componentLocation,
                                                           FlowExceptionHandler exceptionHandler) {
    return internalApplyWithChildContext(Flux.from(eventPub)
        .map(event -> quickCopy(child(((BaseEventContext) event.getContext()), componentLocation, exceptionHandler), event)),
                                         processor, true, true);
  }

  public static Consumer<CoreEvent> completeSuccessIfNeeded(EventContext child, boolean complete) {
    return result -> {
      if (complete && !((BaseEventContext) child).isComplete()) {
        ((BaseEventContext) child).success(result);
      }
    };
  }

  public static Consumer<Throwable> completeErrorIfNeeded(EventContext child, boolean complete) {
    return throwable -> {
      if (complete && !((BaseEventContext) child).isComplete()) {
        ((BaseEventContext) child).error(throwable);
      }
    };
  }

  private static Consumer<CoreEvent> completeSuccessIfNeeded() {
    return result -> {
      final BaseEventContext ctx = (BaseEventContext) result.getContext();
      if (!ctx.isComplete()) {
        ctx.success(result);
      }
    };
  }

  private static Consumer<Either<MessagingException, CoreEvent>> completeSuccessEitherIfNeeded() {
    return result -> result.applyRight(completeSuccessIfNeeded());
  }

  /**
   * Helper method to get the {@link ProcessingStrategy} from a component.
   *
   * @param locator   the locator
   * @param component the component from which root the processing strategy will be obtained
   * @return the processing strategy of the root of the component if it was an instance of {@link ProcessingStrategySupplier},
   *         empty otherwise.
   */
  public static Optional<ProcessingStrategy> getProcessingStrategy(ConfigurationComponentLocator locator,
                                                                   Component component) {
    if (component.getLocation() == null) {
      return empty();
    }

    // TODO MULE-19930 - remove this logic and try to propagate the processing strategy down from the root container
    // instead of doing the lookup.
    return component.getLocation().getParts().get(0).getPartIdentifier()
        // This filter is consistent with the types that implement ProcessingStrategySupplier
        .filter(id -> id.getType().equals(FLOW)
            // a top level router is a policy...
            || id.getType().equals(ROUTER)
            // a top level scope should only be a subflow
            || id.getType().equals(SCOPE))
        .flatMap(id -> getProcessingStrategy(locator, component.getRootContainerLocation()));
  }

  /**
   * Helper method to get the {@link ProcessingStrategy} from a component.
   *
   * @param locator               the locator
   * @param rootContainerLocation the component root container element
   * @return the processing strategy of the root component if it was an instance of {@link ProcessingStrategySupplier}, empty
   *         otherwise.
   *
   * @deprecated Use {@link #getProcessingStrategy(ConfigurationComponentLocator, Component)} instead.
   */
  @Deprecated
  public static Optional<ProcessingStrategy> getProcessingStrategy(ConfigurationComponentLocator locator,
                                                                   Location rootContainerLocation) {
    final Optional<Component> found = locator.find(rootContainerLocation);
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("getProcessingStrategy - location: {} -> found: {}", rootContainerLocation,
                   found.map(loc -> loc.getClass().getSimpleName() + ": " + loc.getLocation().getLocation()).orElse("(null)"));
    }

    return found
        .filter(loc -> loc instanceof ProcessingStrategySupplier)
        .map(loc -> ((ProcessingStrategySupplier) loc).getProcessingStrategy());
  }

  public static ProcessingStrategyFactory getDefaultProcessingStrategyFactory(MuleContext muleContext) {
    return getDefaultProcessingStrategyFactory(muleContext, () -> createDefaultProcessingStrategyFactory());
  }

  public static ProcessingStrategyFactory getDefaultProcessingStrategyFactory(MuleContext muleContext,
                                                                              Supplier<ProcessingStrategyFactory> defaultSupplier) {
    ProcessingStrategyFactory defaultProcessingStrategyFactory =
        muleContext.getConfiguration().getDefaultProcessingStrategyFactory();

    if (defaultProcessingStrategyFactory == null) {
      defaultProcessingStrategyFactory = defaultSupplier.get();
    }

    return defaultProcessingStrategyFactory;
  }

  public static ProcessingStrategyFactory createDefaultProcessingStrategyFactory() {
    return new TransactionAwareProactorStreamEmitterProcessingStrategyFactory();
  }

  /**
   * Transform a given {@link Publisher} using a {@link ReactiveProcessor}. Primarily for use in the implementation of
   * {@link ReactiveProcessor} in other class-loaders.
   *
   * @param publisher the publisher to transform
   * @param processor the processor to transform publisher with
   * @return the transformed publisher
   * @since 4.1
   * @deprecated Since 4.3.0, use {@link RxUtils} instead
   */
  @Deprecated
  public static Publisher<CoreEvent> transform(Publisher<CoreEvent> publisher, ReactiveProcessor processor) {
    return RxUtils.transform(publisher, processor);
  }

  /**
   * Transform a given {@link Publisher} using a mapper function. Primarily for use in the implementation of mapping in other
   * class-loaders.
   *
   * @param publisher the publisher to transform
   * @param mapper    the mapper to map publisher items with
   * @return the transformed publisher
   * @since 4.2
   * @deprecated Since 4.3.0, use {@link RxUtils} instead
   */
  @Deprecated
  public static Publisher<CoreEvent> map(Publisher<CoreEvent> publisher, Function<CoreEvent, CoreEvent> mapper) {
    return RxUtils.map(publisher, mapper);
  }

  /**
   * Perform processing using the provided {@link Function} for each {@link CoreEvent}. Primarily for use in the implementation of
   * {@link ReactiveProcessor} in other class-loaders.
   *
   * @param publisher the publisher to transform
   * @param function  the function to apply to each event.
   * @param component the component that implements this functionality.
   * @return the transformed publisher
   * @since 4.1
   * @deprecated Since 4.3.0, use {@link RxUtils} instead
   */
  @Deprecated
  public static Publisher<CoreEvent> flatMap(Publisher<CoreEvent> publisher,
                                             Function<CoreEvent, Publisher<CoreEvent>> function, Component component) {
    return RxUtils.flatMap(publisher, function, component);
  }

  /**
   * Creates a new {@link Publisher} that will emit the given {@code event}, publishing it on the given {@code executor}.
   *
   * @param event    the {@link CoreEvent} to emit
   * @param executor the thread pool where the event will be published.
   * @return the created publisher
   * @since 4.2
   * @deprecated Since 4.3.0, use {@link RxUtils} instead
   */
  @Deprecated
  public static Publisher<CoreEvent> justPublishOn(CoreEvent event, ExecutorService executor) {
    return RxUtils.justPublishOn(event, executor);
  }
}

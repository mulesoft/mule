/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.lang.Integer.parseInt;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.DataType.NUMBER;
import static org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate.RETRY_COUNT_FOREVER;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static org.mule.runtime.core.api.util.ExceptionUtils.getMessagingExceptionCause;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.applyWithChildContext;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Mono.subscriberContext;
import static reactor.util.context.Context.empty;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.el.ExpressionManagerSession;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.retry.policy.RetryPolicyExhaustedException;
import org.mule.runtime.core.internal.event.EventInternalContextResolver;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.mule.runtime.core.internal.util.rx.ConditionalExecutorServiceDecorator;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;

import reactor.core.publisher.Flux;
import reactor.util.context.Context;

/**
 * Router with {@link UntilSuccessful} retry logic.
 *
 * The retrial chain isolation is implemented using two {@link reactor.core.publisher.FluxSink}s, one for the entry inside the
 * retrial chain, and another for publishing successful events, or exhaustion errors.
 *
 * @since 4.2.3, 4.3.0
 */
class UntilSuccessfulRouter {

  private final Logger LOGGER = getLogger(UntilSuccessfulRouter.class);

  private static final String RETRY_CTX_INTERNAL_PARAM_KEY = "untilSuccessful.router.retryContext";
  private static final String UNTIL_SUCCESSFUL_MSG_PREFIX =
      "'until-successful' retries exhausted. Last exception message was: %s";
  private final EventInternalContextResolver<Map<String, RetryContext>> retryContextResolver;

  private final Component owner;
  private final Predicate<CoreEvent> shouldRetry;
  private final ConditionalExecutorServiceDecorator delayScheduler;

  private final Flux<CoreEvent> upstreamFlux;
  private final Flux<CoreEvent> innerFlux;
  private final Flux<CoreEvent> downstreamFlux;
  private final FluxSinkRecorder<CoreEvent> innerRecorder = new FluxSinkRecorder<>();
  private final FluxSinkRecorder<Either<Throwable, CoreEvent>> downstreamRecorder = new FluxSinkRecorder<>();
  private final AtomicReference<Context> downstreamCtxReference = new AtomicReference(empty());

  // Retry settings, such as the maximum number of retries, and the delay between them
  // are managed by suppliers. By doing this, the implementations remains agnostic of whether
  // they are expressions or not.
  private Function<ExpressionManagerSession, Integer> maxRetriesSupplier;
  private Function<ExpressionManagerSession, Integer> delaySupplier;
  private Function<CoreEvent, ExpressionManagerSession> sessionSupplier;

  // When using an until successful scope in a blocking flow (for example, calling the owner flow with a Processor#process call),
  // this leads to a reactor completion signal being emitted while the event is being re-injected for retrials. This is solved by
  // deferring the downstream publisher completion until all events have evacuated the scope.
  private final AtomicInteger inflightEvents = new AtomicInteger(0);
  private final AtomicBoolean completeDeferred = new AtomicBoolean(false);

  UntilSuccessfulRouter(Component owner, Publisher<CoreEvent> publisher, MessageProcessorChain nestedChain,
                        ExtendedExpressionManager expressionManager, Predicate<CoreEvent> shouldRetry, Scheduler delayScheduler,
                        String maxRetries, String millisBetweenRetries) {
    this.owner = owner;
    this.shouldRetry = shouldRetry;
    this.delayScheduler = new ConditionalExecutorServiceDecorator(delayScheduler, s -> isTransactionActive());
    this.retryContextResolver = new EventInternalContextResolver<>(RETRY_CTX_INTERNAL_PARAM_KEY,
                                                                   HashMap::new);

    // Upstream side of until successful chain. Injects events into retrial chain.
    upstreamFlux = Flux.from(publisher)
        .doOnNext(event -> {
          // Inject event into retrial execution chain
          RetryContext ctx = new RetryContext(event, sessionSupplier, maxRetriesSupplier, delaySupplier);
          inflightEvents.getAndIncrement();
          innerRecorder.next(eventWithCurrentContext(event, ctx));
        })
        .doOnComplete(() -> {
          if (inflightEvents.get() == 0) {
            completeRouter();
          } else {
            completeDeferred.set(true);
          }
        });

    // Inner chain. Contains all retrial and error handling logic.
    innerFlux = Flux.create(innerRecorder)
        // Assume: resolver.currentContextForEvent(publishedEvent) is current context
        .transform(innerPublisher -> applyWithChildContext(innerPublisher, nestedChain,
                                                           Optional.of(owner.getLocation())))
        .doOnNext(successfulEvent -> {
          // Scope execution was successful, pop current ctx
          downstreamRecorder.next(right(Throwable.class, eventWithCurrentContextDeleted(successfulEvent)));
          completeRouterIfNecessary();
        })
        .onErrorContinue(getRetryPredicate(), getRetryHandler());

    // Downstream chain. Unpacks and publishes successful events and errors downstream.
    downstreamFlux = Flux.<Either<Throwable, CoreEvent>>create(sink -> {
      downstreamRecorder.accept(sink);
      // This will always run after the `downstreamCtxReference` is set
      subscribeUpstreamChains(downstreamCtxReference.get());
    })
        .doOnNext(event -> inflightEvents.decrementAndGet())
        .map(getScopeResultMapper());

    if (expressionManager.isExpression(maxRetries)) {
      maxRetriesSupplier = expressionToIntegerSupplierFor(maxRetries);
    } else {
      maxRetriesSupplier = session -> (Integer) parseInt(maxRetries);
    }

    if (expressionManager.isExpression(millisBetweenRetries)) {
      delaySupplier = expressionToIntegerSupplierFor(millisBetweenRetries);
    } else {
      delaySupplier = session -> (Integer) parseInt(millisBetweenRetries);
    }

    // If neither is an expression, we won't need an expression session at all. To keep type consistency, use a
    // no-op expressionSessionSupplier
    if (!expressionManager.isExpression(maxRetries) && !expressionManager.isExpression(millisBetweenRetries)) {
      sessionSupplier = event -> null;
    } else {
      sessionSupplier = event -> expressionManager.openSession(owner.getLocation(), event, NULL_BINDING_CONTEXT);
    }
  }

  private Function<Either<Throwable, CoreEvent>, CoreEvent> getScopeResultMapper() {
    return either -> {
      if (either.isLeft()) {
        throw propagate(either.getLeft());
      } else {
        return either.getRight();
      }
    };
  }

  private BiConsumer<Throwable, Object> getRetryHandler() {
    return (error, offendingEvent) -> {
      MessagingException messagingError = (MessagingException) error;
      RetryContext ctx = getRetryContextForEvent(messagingError.getEvent());
      int retriesLeft =
          ctx.retryCount.getAndDecrement();
      if (retriesLeft > 0) {
        LOGGER.error("Retrying execution of event, attempt {} of {}.", ctx.getAttemptNumber(),
                     ctx.maxRetries != RETRY_COUNT_FOREVER ? ctx.maxRetries : "unlimited");

        // Schedule retry with delay
        UntilSuccessfulRouter.this.delayScheduler.schedule(() -> innerRecorder.next(eventWithCurrentContext(ctx.event, ctx)),
                                                           ctx.delayInMillis, MILLISECONDS);
      } else { // Retries exhausted
        // Current context already pooped. No need to re-insert it
        LOGGER.error("Retry attempts exhausted. Failing...");
        Throwable resolvedError = getThrowableFunction(ctx.event).apply(error);
        // Delete current context from event
        eventWithCurrentContextDeleted(messagingError.getEvent());
        downstreamRecorder.next(left(resolvedError, CoreEvent.class));
        completeRouterIfNecessary();
      }
    };
  }

  /**
   * If there are no events in-flight and the upstream publisher has received a completion signal, complete downstream publishers.
   */
  private void completeRouterIfNecessary() {
    if (completeDeferred.get() && inflightEvents.get() == 0) {
      completeRouter();
    }
  }

  /**
   * Complete both downstream publishers.
   */
  private void completeRouter() {
    innerRecorder.complete();
    downstreamRecorder.complete();
  }

  /**
   * Assembles and returns the downstream {@link Publisher<CoreEvent>}.
   *
   * @return the successful {@link CoreEvent} or retries exhaustion errors {@link Publisher}
   */
  Publisher<CoreEvent> getDownstreamPublisher() {
    return downstreamFlux
        .compose(downstreamPublisher -> subscriberContext()
            .flatMapMany(downstreamContext -> downstreamPublisher.doOnSubscribe(s -> {
              // When a transaction is active, the processing strategy executes the whole reactor chain in the same thread that
              // performs the subscription itself. Because of this, the subscription has to be deferred until the
              // downstreamPublisher FluxCreate#subscribe method registers the new sink in the recorder.
              downstreamCtxReference.set(downstreamContext);
            })));
  }

  private void subscribeUpstreamChains(Context downstreamContext) {
    innerFlux.subscriberContext(downstreamContext).subscribe();
    upstreamFlux.subscriberContext(downstreamContext).subscribe();
  }

  private RetryContext getRetryContextForEvent(CoreEvent event) {
    return retryContextResolver.getCurrentContextFromEvent(event).get(event.getContext().getId());
  }

  /**
   * Insert the ctx as the event's current {@link RetryContext}, and updates the {@link CoreEvent} internal parameters.
   *
   * @param event the current retrial {@link CoreEvent}
   * @param ctx the current {@link RetryContext}
   * @return the {@link CoreEvent} with the retry context saved as internal parameter
   */
  private CoreEvent eventWithCurrentContext(CoreEvent event, RetryContext ctx) {
    // Requires: The ctx that corresponds to this router execution is not in the stack, or there's no stack yet
    // Assures: The ctx that corresponds to this router execution is the one on top of the stack

    Map<String, RetryContext> retryCtxContainer = retryContextResolver.getCurrentContextFromEvent(event);
    retryCtxContainer.put(event.getContext().getId(), ctx);

    return retryContextResolver.eventWithContext(event, retryCtxContainer);
  }

  /**
   * @param event the event from whom to delete the {@link RetryContext}
   * @return the event with the {@link RetryContext} deleted
   */
  private CoreEvent eventWithCurrentContextDeleted(CoreEvent event) {
    Map<String, RetryContext> retryCtxContainer = retryContextResolver.getCurrentContextFromEvent(event);
    retryCtxContainer.remove(event.getContext().getId());
    return retryContextResolver.eventWithContext(event, retryCtxContainer);
  }

  private Predicate<Throwable> getRetryPredicate() {
    return e -> (e instanceof MessagingException && shouldRetry.test(((MessagingException) e).getEvent()));
  }

  private Function<Throwable, Throwable> getThrowableFunction(CoreEvent event) {
    return throwable -> {
      Throwable cause = getMessagingExceptionCause(throwable);
      CoreEvent exceptionEvent = event;
      if (throwable instanceof MessagingException) {
        exceptionEvent = ((MessagingException) throwable).getEvent();
      }
      // RetryPolicyExhaustedException cause is not set in order to avoid RETRY:EXHAUSTED ErrorType replacement
      // during ErrorType resolution (see ChainErrorHandlingUtils and MessagingExceptionResolver)
      return new MessagingException(exceptionEvent,
                                    new RetryPolicyExhaustedException(createStaticMessage(UNTIL_SUCCESSFUL_MSG_PREFIX,
                                                                                          cause.getMessage()),
                                                                      owner),
                                    owner);
    };
  }

  /**
   * Context carrying all retrials information.
   */
  private static class RetryContext {

    CoreEvent event;
    AtomicInteger retryCount = new AtomicInteger();

    Integer delayInMillis;
    Integer maxRetries;

    RetryContext(CoreEvent event,
                 Function<CoreEvent, ExpressionManagerSession> sessionSupplier,
                 Function<ExpressionManagerSession, Integer> maxRetriesSupplier,
                 Function<ExpressionManagerSession, Integer> delayTimeSupplier) {
      this.event = event;

      ExpressionManagerSession session = sessionSupplier.apply(event);
      maxRetries = maxRetriesSupplier.apply(session);
      delayInMillis = delayTimeSupplier.apply(session);
      retryCount.set(maxRetries);
    }

    int getAttemptNumber() {
      return maxRetries - retryCount.get();
    }
  }

  private Function<ExpressionManagerSession, Integer> expressionToIntegerSupplierFor(String anExpression) {
    return session -> {
      try {
        return (Integer) session.evaluate(anExpression, NUMBER).getValue();
      } catch (Exception evaluationException) {
        throw new RetryContextInitializationException(evaluationException);
      }
    };
  }

  /**
   * Wrap all exceptions caused in {@link RetryContext} initialization, so that they can be propagated outside of the innerFlux.
   */
  static class RetryContextInitializationException extends RuntimeException {

    public RetryContextInitializationException(Throwable cause) {
      super(cause);
    }
  }

}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.DataType.NUMBER;
import static org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate.RETRY_COUNT_FOREVER;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static org.mule.runtime.core.api.util.ExceptionUtils.getMessagingExceptionCause;
import static org.mule.runtime.core.internal.event.EventQuickCopy.quickCopy;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.applyWithChildContext;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.Exceptions.propagate;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.el.ExpressionManagerSession;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.retry.policy.RetryPolicyExhaustedException;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.mule.runtime.core.internal.util.rx.ConditionalExecutorServiceDecorator;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 
 * Router with {@link UntilSuccessful} retry logic.
 * 
 * The retrial chain isolation is implemented using two {@link reactor.core.publisher.FluxSink}s, one for the entry inside the
 * retrial chain, and another for publishing successful events, or exhaustion errors.
 * 
 * @since 4.3.0
 */
class UntilSuccessfulRouter {

  private final Logger LOGGER = getLogger(UntilSuccessfulRouter.class);

  private static final String RETRY_CTX_INTERNAL_PARAM_KEY = "RETRY_CTX";
  private static final String UNTIL_SUCCESSFUL_MSG_PREFIX =
      "'until-successful' retries exhausted. Last exception message was: %s";

  private Component owner;
  private Predicate<CoreEvent> shouldRetry;
  private Scheduler delayScheduler;

  private Flux<CoreEvent> upstreamFlux;
  private Flux<CoreEvent> innerFlux;
  private Flux<CoreEvent> downstreamFlux;
  private FluxSinkRecorder<CoreEvent> innerRecorder = new FluxSinkRecorder<>();
  private FluxSinkRecorder<Either<Throwable, CoreEvent>> downstreamRecorder = new FluxSinkRecorder<>();

  // Retry settings, such as the maximum number of retries, and the delay between them
  // are managed by suppliers. By doing this, the implementations remains agnostic of whether
  // they are expressions or not.
  private Function<ExpressionManagerSession, Integer> maxRetriesSupplier;
  private Function<ExpressionManagerSession, Integer> delaySupplier;
  private Function<CoreEvent, ExpressionManagerSession> sessionSupplier;

  UntilSuccessfulRouter(Component owner, Publisher<CoreEvent> publisher, MessageProcessorChain nestedChain,
                        ExtendedExpressionManager expressionManager, Predicate<CoreEvent> shouldRetry, Scheduler delayScheduler,
                        String maxRetries, String millisBetweenRetries) {
    this.owner = owner;
    this.shouldRetry = shouldRetry;
    this.delayScheduler = delayScheduler;

    // Upstream side of until successful chain. Injects events into retrial chain.
    upstreamFlux = Flux.from(publisher)
        .doOnNext(event -> {
          // Inject event into retrial execution chain
          innerRecorder.next(eventWithCtx(event, new RetryContext(event)));
        })
        .doOnComplete(() -> {
          innerRecorder.complete();
          downstreamRecorder.complete();
        });

    // Inner chain. Contains all retrial and error handling logic.
    innerFlux = Flux.create(innerRecorder)
        .transform(innerPublisher -> applyWithChildContext(innerPublisher, nestedChain,
                                                           Optional.of(owner.getLocation())))
        .doOnNext(successfulEvent -> downstreamRecorder.next(Either.right(Throwable.class, successfulEvent)))
        .onErrorContinue(getRetryPredicate(), (error, offendingEvent) -> {
          MessagingException messagingError = (MessagingException) error;
          RetryContext ctx =
              ((InternalEvent) messagingError.getEvent()).getInternalParameter(RETRY_CTX_INTERNAL_PARAM_KEY);
          int retriesLeft =
              ctx.retryCount.getAndDecrement();
          if (retriesLeft > 0) {
            LOGGER.error("Retrying execution of event, attempt {} of {}.", ctx.getRetriesLeft(),
                         ctx.maxRetries != RETRY_COUNT_FOREVER ? ctx.maxRetries : "unlimited");

            // Schedule retry with delay
            ctx.delayScheduler.schedule(() -> innerRecorder.next(eventWithCtx(ctx.event, ctx)),
                                        ctx.delayInMillis, TimeUnit.MILLISECONDS);
          } else { // Retries exhausted
            LOGGER.error("Retry attempts exhausted. Failing...");
            Throwable resolvedError = getThrowableFunction(ctx.event).apply(error);
            downstreamRecorder.next(Either.left(resolvedError, CoreEvent.class));
          }
        });

    // Downstream chain. Unpacks and publishes successful events and errors downstream.
    downstreamFlux = Flux.create(downstreamRecorder)
        .map(either -> {
          if (either.isLeft()) {
            throw propagate(either.getLeft());
          } else {
            return either.getRight();
          }
        });

    if (expressionManager.isExpression(maxRetries)) {
      maxRetriesSupplier = expressionToIntegerSupplierFor(maxRetries);
    } else {
      Integer asInteger = Integer.parseInt(maxRetries);
      maxRetriesSupplier = session -> asInteger;
    }

    if (expressionManager.isExpression(millisBetweenRetries)) {
      delaySupplier = expressionToIntegerSupplierFor(millisBetweenRetries);
    } else {
      Integer asInteger = Integer.parseInt(millisBetweenRetries);
      delaySupplier = session -> asInteger;
    }

    // If neither is an expression, we won't need an expression session at all. To keep type consistency, use a
    // no-op expressionSessionSupplier
    if (!expressionManager.isExpression(maxRetries) && !expressionManager.isExpression(millisBetweenRetries)) {
      sessionSupplier = event -> null;
    } else {
      sessionSupplier = event -> expressionManager.openSession(owner.getLocation(), event, NULL_BINDING_CONTEXT);
    }
  }

  /**
   * Assembles and returns the downstream {@link Publisher<CoreEvent>}.
   * 
   * @return the successful {@link CoreEvent} or retries exhaustion errors {@link Publisher}
   */
  Publisher<CoreEvent> getDownstreamPublisher() {
    return downstreamFlux
        .compose(downstreamPublisher -> Mono.subscriberContext()
            .flatMapMany(downstreamContext -> downstreamPublisher.doOnSubscribe(s -> {
              innerFlux.subscriberContext(downstreamContext).subscribe();
              upstreamFlux.subscriberContext(downstreamContext).subscribe();
            })));
  }


  /**
   * Saves the {@link RetryContext} inside the event being routed through the retrial chain.
   * 
   * @param event the current retrial {@link CoreEvent}
   * @param ctx the current {@link RetryContext}
   * @return the {@link CoreEvent} with the retry context saved as internal parameter
   */
  private CoreEvent eventWithCtx(CoreEvent event, RetryContext ctx) {

    // TODO: There must be cleaner way to do this

    Map<String, Object> parametersWithCtx = new HashMap<>();
    parametersWithCtx.put(RETRY_CTX_INTERNAL_PARAM_KEY, ctx);
    return quickCopy(event, parametersWithCtx);
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
      return new MessagingException(exceptionEvent,
                                    new RetryPolicyExhaustedException(createStaticMessage(UNTIL_SUCCESSFUL_MSG_PREFIX,
                                                                                          cause.getMessage()),
                                                                      cause, owner),
                                    owner);
    };
  }

  /**
   * Context carrying all retrials information.
   */
  class RetryContext {

    // TODO: Should handle nested Until Successful scope executions in some way

    CoreEvent event;
    AtomicInteger retryCount = new AtomicInteger();

    Integer delayInMillis;
    Integer maxRetries;
    ConditionalExecutorServiceDecorator delayScheduler;

    RetryContext(CoreEvent event) {
      this.event = event;

      ExpressionManagerSession session = sessionSupplier.apply(event);

      maxRetries = maxRetriesSupplier.apply(session);

      delayInMillis = delaySupplier.apply(session);

      retryCount.set(maxRetries);

      // TODO: Is it necessary to create this executor in each retry ctx?
      delayScheduler =
          new ConditionalExecutorServiceDecorator(UntilSuccessfulRouter.this.delayScheduler, s -> isTransactionActive());
    }

    int getRetriesLeft() {
      return maxRetries - retryCount.get() + 1;
    }
  }

  private Function<ExpressionManagerSession, Integer> expressionToIntegerSupplierFor(String anExpression) {
    return session -> (Integer) session.evaluate(anExpression, NUMBER).getValue();
  }

}

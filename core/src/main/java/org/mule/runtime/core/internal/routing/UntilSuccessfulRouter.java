/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate.RETRY_COUNT_FOREVER;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static org.mule.runtime.core.api.util.ExceptionUtils.getMessagingExceptionCause;
import static org.mule.runtime.core.internal.event.EventQuickCopy.quickCopy;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.applyWithChildContext;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.Exceptions.propagate;
import org.mule.runtime.api.metadata.DataType;
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

class UntilSuccessfulRouter {

  private final Logger LOGGER = getLogger(UntilSuccessfulRouter.class);

  private static final String RETRY_CTX_INTERNAL_PARAM_KEY = "RETRY_CTX";

  private static final String UNTIL_SUCCESSFUL_MSG_PREFIX =
      "'until-successful' retries exhausted. Last exception message was: %s";

  private UntilSuccessful untilSuccessful;
  FluxSinkRecorder<CoreEvent> innerRecorder = new FluxSinkRecorder<>();
  FluxSinkRecorder<Either<Throwable, CoreEvent>> downstreamRecorder = new FluxSinkRecorder<>();

  Flux<CoreEvent> upstreamFlux;
  private Predicate<CoreEvent> shouldRetry;
  private Scheduler delayScheduler;
  private String maxRetries;
  private String millisBetweenRetries;
  Flux<CoreEvent> innerFlux;
  private MessageProcessorChain nestedChain;
  private ExtendedExpressionManager expressionManager;
  Flux<CoreEvent> downstreamFlux;

  UntilSuccessfulRouter(UntilSuccessful untilSuccessful, Publisher<CoreEvent> publisher, MessageProcessorChain nestedChain,
                        ExtendedExpressionManager expressionManager, Predicate<CoreEvent> shouldRetry, Scheduler delayScheduler,
                        String maxRetries, String millisBetweenRetries) {
    this.untilSuccessful = untilSuccessful;
    upstreamFlux = Flux.from(publisher)
        .doOnNext(event -> {
          // publish event
          LOGGER.error("Setting new context for event {} and injecting in router {}", event.getCorrelationId(),
                       this.toString());
          innerRecorder.next(eventWithCtx(event, new RetryContext(event)));
        })
        .doOnComplete(() -> {
          // complete next stage of chain
          innerRecorder.complete();
          downstreamRecorder.complete();
        });
    this.shouldRetry = shouldRetry;
    this.delayScheduler = delayScheduler;
    this.maxRetries = maxRetries;
    this.millisBetweenRetries = millisBetweenRetries;

    innerFlux = Flux.create(innerRecorder)
        .transform(innerPublisher -> applyWithChildContext(innerPublisher, nestedChain,
                                                           Optional.of(untilSuccessful.getLocation())))
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
    this.nestedChain = nestedChain;
    this.expressionManager = expressionManager;

    downstreamFlux = Flux.create(downstreamRecorder)
        .map(either -> {
          if (either.isLeft()) {
            throw propagate(either.getLeft());
          } else {
            return either.getRight();
          }
        });
  }

  Publisher<CoreEvent> getDownstreamPublisher() {
    return downstreamFlux.compose(downstream -> Mono.subscriberContext().flatMapMany(dsCtx -> downstream.doOnSubscribe(s -> {
      innerFlux.subscriberContext(dsCtx).subscribe();
      upstreamFlux.subscriberContext(dsCtx).subscribe();
    })));
  }


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
                                                                      cause, untilSuccessful),
                                    untilSuccessful);
    };
  }

  class RetryContext {

    // TODO: Should handle nested Until Successful scope executions in some way

    CoreEvent event;
    AtomicInteger retryCount = new AtomicInteger();

    Integer delayInMillis;
    Integer maxRetries;
    ConditionalExecutorServiceDecorator delayScheduler;

    RetryContext(CoreEvent event) {
      this.event = event;

      ExpressionManagerSession session =
          expressionManager.openSession(untilSuccessful.getLocation(), event, NULL_BINDING_CONTEXT);

      // Max retries: Expression or literal
      if (expressionManager.isExpression(UntilSuccessfulRouter.this.maxRetries)) {
        maxRetries = (Integer) session.evaluate(UntilSuccessfulRouter.this.maxRetries, DataType.NUMBER).getValue();
      } else {
        maxRetries = Integer.parseInt(UntilSuccessfulRouter.this.maxRetries);
      }

      // Delay between retries: Expression or literal
      if (expressionManager.isExpression(millisBetweenRetries)) {
        delayInMillis =
            (Integer) session.evaluate(UntilSuccessfulRouter.this.millisBetweenRetries, DataType.NUMBER).getValue();
      } else {
        delayInMillis = Integer.parseInt(millisBetweenRetries);
      }

      retryCount.set(maxRetries);

      // TODO: Is it necessary to create this executor in each retry ctx?
      delayScheduler =
          new ConditionalExecutorServiceDecorator(UntilSuccessfulRouter.this.delayScheduler, s -> isTransactionActive());
    }

    int getRetriesLeft() {
      return maxRetries - retryCount.get() + 1;
    }
  }



}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate.RETRY_COUNT_FOREVER;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static org.mule.runtime.core.api.util.ExceptionUtils.getMessagingExceptionCause;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.applyWithChildContext;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.buildNewChainWithListOfProcessors;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getProcessingStrategy;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Flux.from;
import static reactor.core.scheduler.Schedulers.fromExecutorService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.el.ExpressionManagerSession;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.processor.AbstractMuleObjectOwner;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.policy.NoRetryPolicyTemplate;
import org.mule.runtime.core.api.retry.policy.RetryPolicyExhaustedException;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.mule.runtime.core.internal.util.rx.ConditionalExecutorServiceDecorator;
import org.mule.runtime.core.privileged.processor.MessageProcessors;
import org.mule.runtime.core.privileged.processor.Scope;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import com.github.benmanes.caffeine.cache.LoadingCache;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.retry.RetryExhaustedException;
import reactor.util.context.Context;

/**
 * UntilSuccessful attempts to innerFlux a message to the message processor it contains. Routing is considered successful if no
 * exception has been raised and, optionally, if the response matches an expression.
 */
public class UntilSuccessful extends AbstractMuleObjectOwner implements Scope {

  private final Logger LOGGER = getLogger(UntilSuccessful.class);

  private static final String UNTIL_SUCCESSFUL_MSG_PREFIX =
      "'until-successful' retries exhausted. Last exception message was: %s";
  private static final String DEFAULT_MILLIS_BETWEEN_RETRIES = "60000";
  private static final String DEFAULT_RETRIES = "5";

  @Inject
  private SchedulerService schedulerService;

  @Inject
  private ExtendedExpressionManager expressionManager;

  private String maxRetries = DEFAULT_RETRIES;
  private String millisBetweenRetries = DEFAULT_MILLIS_BETWEEN_RETRIES;
  private MessageProcessorChain nestedChain;
  private Predicate<CoreEvent> shouldRetry;
  private Optional<RetryPolicyTemplate> policyTemplate;
  private LoadingCache<Pair<Integer, Integer>, RetryPolicyTemplate> policyTemplatesCache =
      newBuilder().build(p -> new SimpleRetryPolicyTemplate(p.getFirst(), p.getSecond()));
  private Scheduler timer;
  private List<Processor> processors;
  private int maxRetriesAsInteger;
  private Duration timeBetweenRetries;

  @Override
  public void initialise() throws InitialisationException {
    if (processors == null) {
      throw new InitialisationException(createStaticMessage("One message processor must be configured within 'until-successful'."),
                                        this);
    }
    this.nestedChain = buildNewChainWithListOfProcessors(getProcessingStrategy(locator, getRootContainerLocation()), processors);
    super.initialise();
    timer = schedulerService.cpuLightScheduler();

    // In case both 'maxRetries' and 'millisBetweenRetries' are not expressions (or just 'maxRetries' is 0), then
    // there is no need to calculate expressions each time, so we create the (unique) RetryPolicyTemplate here
    // In other case, then the policy template will be calculated each time (using the policyTemplatesCache)
    policyTemplate = empty();
    if (!expressionManager.isExpression(this.maxRetries)) {
      int maxRetries = Integer.parseInt(this.maxRetries);
      if (maxRetries == 0) {
        policyTemplate = of(new NoRetryPolicyTemplate());
      } else if (!expressionManager.isExpression(this.millisBetweenRetries)) {
        long millisBetweenRetries = Long.parseLong(this.millisBetweenRetries);
        timeBetweenRetries = Duration.ofMillis(millisBetweenRetries);
        policyTemplate = of(new SimpleRetryPolicyTemplate(millisBetweenRetries, maxRetries));
      }
      maxRetriesAsInteger = maxRetries;
    }
    shouldRetry = event -> event.getError().isPresent();
  }

  @Override
  public void dispose() {
    super.dispose();
    timer.stop();
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return processToApply(event, this);
  }

  private RetryPolicyTemplate createRetryPolicyTemplate(CoreEvent event) {
    ExpressionManagerSession session = expressionManager.openSession(getLocation(), event, NULL_BINDING_CONTEXT);
    Integer maxRetries = (Integer) session.evaluate(this.maxRetries, DataType.NUMBER).getValue();
    Integer millisBetweenRetries = (Integer) session.evaluate(this.millisBetweenRetries, DataType.NUMBER).getValue();
    return this.policyTemplatesCache.get(new Pair<>(millisBetweenRetries, maxRetries));
  }

  private RetryPolicyTemplate fetchPolicyTemplate(CoreEvent event) {
    return policyTemplate.orElseGet(() -> createRetryPolicyTemplate(event));
  }

  class RetryContext {

    private int maxRetries;
    CoreEvent event;
    private AtomicInteger retryCount = new AtomicInteger();
    private Duration delay;

    RetryContext(CoreEvent event) {
      this.event = event;

      Integer maxRetriesEvaluated;
      Duration chosenDuration;

      ExpressionManagerSession session = expressionManager.openSession(getLocation(), event, NULL_BINDING_CONTEXT);

      if (expressionManager.isExpression(UntilSuccessful.this.maxRetries)) {
        Integer maxRetries = (Integer) session.evaluate(UntilSuccessful.this.maxRetries, DataType.NUMBER).getValue();
        maxRetriesEvaluated = maxRetries;
      } else {
        maxRetriesEvaluated = maxRetriesAsInteger;
      }

      if (expressionManager.isExpression(millisBetweenRetries)) {
        Integer millisBetweenRetries =
            (Integer) session.evaluate(UntilSuccessful.this.millisBetweenRetries, DataType.NUMBER).getValue();
        chosenDuration = Duration.ofMillis(millisBetweenRetries);
      } else {
        chosenDuration = Duration.ofMillis(Integer.parseInt(millisBetweenRetries));
      }

      retryCount.set(maxRetriesEvaluated);
      maxRetries = maxRetriesEvaluated;
      delay = chosenDuration;
    }
  }

  class UntilSuccessfulRouter {

    Flux<CoreEvent> upstreamFlux;
    Flux<CoreEvent> downstreamFlux;
    FluxSinkRecorder<CoreEvent> innerRecorder = new FluxSinkRecorder<>();
    FluxSinkRecorder<Either<CoreEvent, Throwable>> downstreamRecorder = new FluxSinkRecorder<>();
    Flux<CoreEvent> innerFlux;
    RetryContext currentContext;
    AtomicReference<reactor.util.context.Context> downstreamCtxRef = new AtomicReference<>();

    UntilSuccessfulRouter(Publisher<CoreEvent> publisher) {
      upstreamFlux = Flux.from(publisher)
          .doOnNext(event -> {
            // publish event

            currentContext = new RetryContext(event);

            innerRecorder.next(event);
          })
          .doOnComplete(() -> {
            // complete next stage of chain
            currentContext = null;
            innerRecorder.complete();
          });

      final LazyValue<Boolean> isTransactional = new LazyValue<>(() -> isTransactionActive());
      reactor.core.scheduler.Scheduler reactorRetryScheduler =
          fromExecutorService(new ConditionalExecutorServiceDecorator(timer, s -> isTransactional.get()));

      innerFlux = Flux.create(innerRecorder)
          .transform(publisher1 -> applyWithChildContext(Flux.from(publisher1), nestedChain,
                                                         Optional.of(UntilSuccessful.this.getLocation())))
          // Success, inject into downstream publisher
          .doOnNext(successEvent -> downstreamRecorder.next(Either.left(successEvent)))
          // Have to check if the context reaches the onErrorContinue
          .onErrorContinue(getRetryPredicate(), (error, failureEvent) -> {
            int retriesLeft =
                currentContext.retryCount.getAndDecrement();
            if (retriesLeft > 0) { // Retry
              LOGGER.error("Retrying execution of event, attempt {} of {}.", currentContext.maxRetries - retriesLeft + 1,
                           maxRetriesAsInteger != RETRY_COUNT_FOREVER ? currentContext.maxRetries : "unlimited");
              innerRecorder.next(currentContext.event);
            } else { // Retries exhausted
              LOGGER.error("Retry attempts exhausted. Failing...");
              Throwable resolvedError = getThrowableFunction(currentContext.event).apply(new RetryExhaustedException(error));
              downstreamRecorder.next(Either.right(CoreEvent.class, resolvedError));
            }
          })
          .onErrorMap(RetryExhaustedException.class,
                      re -> getThrowableFunction(currentContext.event).apply(unwrap(re.getCause())))
          // TODO: Check how to implement this delay without the concatMap operator?
          .concatMap(event -> Mono.just(event).delayElement(currentContext.delay, reactorRetryScheduler))
          .doOnComplete(() -> downstreamRecorder.complete());

      downstreamFlux = Flux.create(downstreamRecorder)
          .map(either -> {
            if (either.isLeft()) {
              return either.getLeft();
            } else {
              throw propagate(either.getRight());
            }
          });


      innerFlux =
          routeSubscriptionAffectingCtxWith(upstreamFlux, innerFlux, innerCtx -> Context.empty().putAll(downstreamCtxRef.get()));

      // Decorate all fluxes
      downstreamFlux = routeSubscriptionAffectingCtxWith(innerFlux, downstreamFlux, downstreamCtx -> {
        downstreamCtxRef.set(downstreamCtx);
        return Context.empty().putAll(downstreamCtx);
      });

    }
  }


  private Flux<CoreEvent> routeSubscriptionAffectingCtxWith(Flux<CoreEvent> upstream, Flux<CoreEvent> downstream,
                                                            Function<Context, Context> contextFunction) {
    return downstream.compose(eventPub -> Mono.subscriberContext()
        .flatMapMany(ctx -> eventPub.doOnSubscribe(s -> upstream.subscriberContext(contextFunction).subscribe())));
  }


  public Publisher<CoreEvent> alternativeApply(Publisher<CoreEvent> publisher) {
    UntilSuccessfulRouter router = new UntilSuccessfulRouter(publisher);
    return router.downstreamFlux;

    // return router.downstreamFlux;

    // return subscribeFluxOnPublisherSubscription(router.innerFlux, router.upstreamFlux);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return alternativeApply(publisher);
    // return mule4xApply(publisher);
  }

  public Publisher<CoreEvent> mule4xApply(Publisher<CoreEvent> publisher) {
    return from(publisher)
        .flatMap(event -> Mono
            .from(MessageProcessors.processWithChildContextDontComplete(event, nestedChain, ofNullable(getLocation())))
            .transform(p -> fetchPolicyTemplate(event).applyPolicy(p, getRetryPredicate(), e -> {
            }, getThrowableFunction(event), timer)));
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
                                                                      cause, this),
                                    this);
    };
  }

  /**
   * @return the number of retries to process the innerFlux when failing. Default value is 5.
   */
  public String getMaxRetries() {
    return maxRetries;
  }

  /**
   *
   * @param maxRetries the number of retries to process the innerFlux when failing. Default value is 5.
   */
  public void setMaxRetries(final String maxRetries) {
    this.maxRetries = maxRetries;
  }

  /**
   * @return the number of milliseconds between retries. Default value is 60000.
   */
  public String getMillisBetweenRetries() {
    return millisBetweenRetries;
  }

  /**
   * @param millisBetweenRetries the number of milliseconds between retries. Default value is 60000.
   */
  public void setMillisBetweenRetries(String millisBetweenRetries) {
    this.millisBetweenRetries = millisBetweenRetries;
  }

  /**
   * Configure the nested {@link Processor}'s that error handling and transactional behaviour should be applied to.
   *
   * @param processors
   */
  public void setMessageProcessors(List<Processor> processors) {
    this.processors = processors;
  }

  @Override
  protected List<Object> getOwnedObjects() {
    return singletonList(nestedChain);
  }
}


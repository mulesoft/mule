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
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate.RETRY_COUNT_FOREVER;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static org.mule.runtime.core.api.util.ExceptionUtils.getMessagingExceptionCause;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.applyWithChildContext;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.buildNewChainWithListOfProcessors;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getProcessingStrategy;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.Exceptions.propagate;
import static reactor.core.Exceptions.unwrap;
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
import org.mule.runtime.core.api.processor.AbstractMuleObjectOwner;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.policy.NoRetryPolicyTemplate;
import org.mule.runtime.core.api.retry.policy.RetryPolicyExhaustedException;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.util.rx.ConditionalExecutorServiceDecorator;
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
import reactor.core.publisher.FluxSink;
import reactor.retry.RetryExhaustedException;

/**
 * UntilSuccessful attempts to route a message to the message processor it contains. Routing is considered successful if no
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


  public Publisher<CoreEvent> alternativeApply(Publisher<CoreEvent> publisher) {
    return from(publisher)
        .flatMap(event -> {
          AtomicInteger retryCounter = new AtomicInteger(maxRetriesAsInteger + 1);
          Duration choosenDuration = timeBetweenRetries;
          Integer maxRetriesEvaluated = 0;

          if (expressionManager.isExpression(maxRetries)) {
            ExpressionManagerSession session = expressionManager.openSession(getLocation(), event, NULL_BINDING_CONTEXT);
            Integer maxRetries = (Integer) session.evaluate(this.maxRetries, DataType.NUMBER).getValue();
            maxRetriesEvaluated = maxRetries;
            retryCounter.set(maxRetries + 1);
            if (expressionManager.isExpression(millisBetweenRetries)) {
              Integer millisBetweenRetries = (Integer) session.evaluate(this.millisBetweenRetries, DataType.NUMBER).getValue();
              choosenDuration = Duration.ofMillis(millisBetweenRetries);
            } else {
              choosenDuration = Duration.ofMillis(Integer.parseInt(millisBetweenRetries));
            }
          }

          final LazyValue<Boolean> isTransactional = new LazyValue<>(() -> isTransactionActive());
          reactor.core.scheduler.Scheduler reactorRetryScheduler =
              fromExecutorService(new ConditionalExecutorServiceDecorator(timer, s -> isTransactional.get()));

          AtomicReference<FluxSink> sinkReference = new AtomicReference<>();
          Integer finalMaxRetriesEvaluated = maxRetriesEvaluated;
          return Flux.<CoreEvent>create(sink -> {
            sinkReference.set(sink);
            sink.next(event);
          })
              .transform(publisher1 -> applyWithChildContext(publisher1, nestedChain,
                                                             Optional.of(UntilSuccessful.this.getLocation())))
              .doOnNext(completedEvent -> sinkReference.get().complete())
              .onErrorContinue(getRetryPredicate(), (error, failureEvent) -> {
                int retriesLeft = retryCounter.decrementAndGet();
                if (retriesLeft > 0) {
                  // Retry
                  LOGGER.error("Retrying execution of event, attempt {} of {}.", retriesLeft,
                               maxRetriesAsInteger != RETRY_COUNT_FOREVER ? finalMaxRetriesEvaluated.toString() : "unlimited");
                  sinkReference.get().next(event);
                } else {
                  // Retries exhausted
                  LOGGER.error("Retry attempts exhausted. Failing...");
                  throw propagate(new RetryExhaustedException(error));
                }
              })
              .onErrorMap(RetryExhaustedException.class, re -> getThrowableFunction(event).apply(unwrap(re.getCause())))
              .delayElements(choosenDuration, reactorRetryScheduler);
        });
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return alternativeApply(publisher);
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
   * @return the number of retries to process the route when failing. Default value is 5.
   */
  public String getMaxRetries() {
    return maxRetries;
  }

  /**
   *
   * @param maxRetries the number of retries to process the route when failing. Default value is 5.
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


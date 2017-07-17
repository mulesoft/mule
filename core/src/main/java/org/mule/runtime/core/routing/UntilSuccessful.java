/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static java.lang.Thread.sleep;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.processor.MessageProcessors.newChain;
import static org.mule.runtime.core.api.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.api.processor.MessageProcessors.processToApplyWithChildContext;
import static org.mule.runtime.core.api.processor.MessageProcessors.processWithChildContext;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static org.mule.runtime.core.routing.outbound.AbstractOutboundRouter.DEFAULT_FAILURE_EXPRESSION;
import static reactor.core.publisher.Flux.empty;
import static reactor.core.publisher.Flux.error;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.just;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Scope;
import org.mule.runtime.core.api.retry.policy.RetryPolicyExhaustedException;
import org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate;
import org.mule.runtime.core.api.store.ListableObjectStore;
import org.mule.runtime.core.processor.AbstractMuleObjectOwner;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * UntilSuccessful attempts to route a message to the message processor it contains. Routing is considered successful if no
 * exception has been raised and, optionally, if the response matches an expression.
 * <p>
 * UntilSuccessful internal route can be executed synchronously or asynchronously depending on the threading profile defined on
 * it. By default, if no threading profile is defined, then it will use the default threading profile configuration for the
 * application. This means that the default behavior is to process asynchronously.
 * <p>
 * UntilSuccessful can optionally be configured to synchronously return an acknowledgment message when it has scheduled the event
 * for processing. UntilSuccessful is backed by a {@link ListableObjectStore} for storing the events that are pending
 * (re)processing.
 */
public class UntilSuccessful extends AbstractMuleObjectOwner implements Scope {

  private static final Logger LOGGER = LoggerFactory.getLogger(UntilSuccessful.class);

  private static final String UNTIL_SUCCESSFUL_MSG_PREFIX = "until-successful retries exhausted. Last exception message was: %s";
  private static final String EXPRESSION_FAILED_MSG = "Failure expression positive when processing event: ";
  private static final long DEFAULT_MILLIS_BETWEEN_RETRIES = 60 * 1000;
  private static final int DEFAULT_RETRIES = 5;

  private int maxRetries = DEFAULT_RETRIES;
  private Long millisBetweenRetries = DEFAULT_MILLIS_BETWEEN_RETRIES;
  private String failureExpression = DEFAULT_FAILURE_EXPRESSION;
  private MessageProcessorChain nestedChain;
  private Predicate<Event> shouldRetry;
  private SimpleRetryPolicyTemplate policyTemplate;
  private Scheduler timer;

  @Override
  public void initialise() throws InitialisationException {
    if (nestedChain == null) {
      throw new InitialisationException(createStaticMessage("One message processor must be configured within UntilSuccessful."),
                                        this);
    }
    super.initialise();
    timer = muleContext.getSchedulerService().cpuLightScheduler();
    policyTemplate =
        new SimpleRetryPolicyTemplate(millisBetweenRetries, maxRetries, timer);
    shouldRetry = event -> (muleContext.getExpressionManager().evaluateBoolean(failureExpression, event,
                                                                               getLocation(), false, true));
  }

  @Override
  public void dispose() {
    super.dispose();
    timer.stop();
  }

  @Override
  public Event process(Event event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public Publisher<Event> apply(Publisher<Event> publisher) {
    if (isTransactionActive()) {
      return from(publisher).flatMap(event -> {
        try {
          Exception lastExecutionException = null;
          for (int i = 0; i <= maxRetries; i++) {
            try {
              Event result = processToApplyWithChildContext(event, nestedChain);
              if (result == null) {
                return empty();
              }
              if (shouldRetry.test(result)) {
                throw new ExpressionFailureException(createStaticMessage(EXPRESSION_FAILED_MSG + event));
              } else {
                return just(result);
              }
            } catch (Exception e) {
              if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Exception thrown inside until-successful ", e);
              }
              if (getRetryPredicate().test(e)) {
                lastExecutionException = e;
                if (i < maxRetries) {
                  sleep(millisBetweenRetries);
                }
              } else {
                return error(e);
              }
            }
          }
          return error(getThrowableFunction(event).apply(lastExecutionException));
        } catch (Exception e) {
          return error(e);
        }
      });
    } else {
      return from(publisher)
          .flatMap(event -> Mono
              .from(processWithChildContext(event, scheduleRoute(p -> Mono.from(p)
                  .transform(nestedChain)
                  .doOnNext(result -> {
                    if (shouldRetry.test(result)) {
                      throw new ExpressionFailureException(createStaticMessage(EXPRESSION_FAILED_MSG + event));
                    }
                  })), ofNullable(getLocation())))
              .transform(p -> policyTemplate.applyPolicy(p, getRetryPredicate(), e -> {
              }, getThrowableFunction(event))));
    }
  }

  private Predicate<Throwable> getRetryPredicate() {
    return e -> e instanceof ExpressionFailureException
        || (e instanceof MessagingException && shouldRetry.test(((MessagingException) e).getEvent()));
  }

  private Function<Throwable, Throwable> getThrowableFunction(Event event) {
    return throwable -> {
      Throwable cause = throwable instanceof MessagingException ? throwable.getCause() : throwable;
      return new MessagingException(event,
                                    new RetryPolicyExhaustedException(createStaticMessage(UNTIL_SUCCESSFUL_MSG_PREFIX,
                                                                                          cause.getMessage()),
                                                                      cause, this),
                                    this);
    };
  }

  private ReactiveProcessor scheduleRoute(ReactiveProcessor route) {
    if (flowConstruct instanceof Pipeline) {
      // If an async processing strategy is in use then use it to schedule scatter-gather route
      return publisher -> from(publisher).transform(((Pipeline) flowConstruct).getProcessingStrategy().onPipeline(route));
    } else {
      return publisher -> publisher;
    }
  }

  /**
   * @return the number of retries to process the route when failing. Default value is 5.
   */
  public int getMaxRetries() {
    return maxRetries;
  }

  /**
   *
   * @param maxRetries the number of retries to process the route when failing. Default value is 5.
   */
  public void setMaxRetries(final int maxRetries) {
    this.maxRetries = maxRetries;
  }

  /**
   * @return the number of milliseconds between retries. Default value is 60000.
   */
  public long getMillisBetweenRetries() {
    return millisBetweenRetries;
  }

  /**
   * @param millisBetweenRetries the number of milliseconds between retries. Default value is 60000.
   */
  public void setMillisBetweenRetries(long millisBetweenRetries) {
    this.millisBetweenRetries = millisBetweenRetries;
  }

  /**
   * @return Expression to determine if the message was processed successfully or not. Always returns a not null value.
   */
  public String getFailureExpression() {
    return failureExpression;
  }

  /**
   * @param failureExpression Expression to determine if the message was processed successfully or not. Always returns a not null
   *        value.
   */
  public void setFailureExpression(final String failureExpression) {
    this.failureExpression = failureExpression;
  }

  /**
   * Configure the nested {@link Processor}'s that error handling and transactional behaviour should be applied to.
   *
   * @param processors
   */
  public void setMessageProcessors(List<Processor> processors) {
    this.nestedChain = newChain(processors);
  }

  @Override
  protected List<Object> getOwnedObjects() {
    return singletonList(nestedChain);
  }

  private static class ExpressionFailureException extends MuleRuntimeException {

    public ExpressionFailureException(I18nMessage message) {
      super(message);
    }
  }
}


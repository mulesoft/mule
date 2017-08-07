/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.processor.MessageProcessors.newChain;
import static org.mule.runtime.core.api.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.api.processor.MessageProcessors.processWithChildContext;
import static org.mule.runtime.core.api.util.ExceptionUtils.getMessagingExceptionCause;
import static org.mule.runtime.core.privileged.routing.outbound.AbstractOutboundRouter.DEFAULT_FAILURE_EXPRESSION;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.processor.AbstractMuleObjectOwner;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Router;
import org.mule.runtime.core.api.retry.policy.RetryPolicyExhaustedException;
import org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * UntilSuccessful attempts to route a message to the message processor it contains. Routing is considered successful if no
 * exception has been raised and, optionally, if the response matches an expression.
 */
public class UntilSuccessful extends AbstractMuleObjectOwner implements Router {

  private static final Logger LOGGER = LoggerFactory.getLogger(UntilSuccessful.class);

  private static final String UNTIL_SUCCESSFUL_MSG_PREFIX =
      "'until-successful' retries exhausted. Last exception message was: %s";
  private static final String EXPRESSION_FAILED_MSG = "Failure expression positive when processing event: ";
  private static final long DEFAULT_MILLIS_BETWEEN_RETRIES = 60 * 1000;
  private static final int DEFAULT_RETRIES = 5;

  @Inject
  private ConfigurationComponentLocator componentLocator;

  private int maxRetries = DEFAULT_RETRIES;
  private Long millisBetweenRetries = DEFAULT_MILLIS_BETWEEN_RETRIES;
  private String failureExpression = DEFAULT_FAILURE_EXPRESSION;
  private MessageProcessorChain nestedChain;
  private Predicate<Event> shouldRetry;
  private SimpleRetryPolicyTemplate policyTemplate;
  private Scheduler timer;
  private FlowConstruct flowConstruct;

  @Override
  public void initialise() throws InitialisationException {
    if (nestedChain == null) {
      throw new InitialisationException(createStaticMessage("One message processor must be configured within 'until-successful'."),
                                        this);
    }
    super.initialise();
    timer = muleContext.getSchedulerService().cpuLightScheduler();
    policyTemplate =
        new SimpleRetryPolicyTemplate(millisBetweenRetries, maxRetries, timer);
    shouldRetry = event -> (muleContext.getExpressionManager().evaluateBoolean(failureExpression, event,
                                                                               getLocation(), false, true));
    flowConstruct = (FlowConstruct) componentLocator.find(Location.builder().globalName(getRootContainerName()).build()).get();
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
    return from(publisher)
        .flatMap(event -> Mono
            .from(processWithChildContext(event, scheduleRoute(p -> Mono.from(p)
                .transform(nestedChain)
                .doOnNext(result -> {
                  if (shouldRetry.test(result)) {
                    throw new FailureExpressionAssertionException(createStaticMessage(EXPRESSION_FAILED_MSG + event));
                  }
                })), ofNullable(getLocation())))
            .transform(p -> policyTemplate.applyPolicy(p, getRetryPredicate(), e -> {
            }, getThrowableFunction(event))));
  }

  private Predicate<Throwable> getRetryPredicate() {
    return e -> e instanceof FailureExpressionAssertionException
        || (e instanceof MessagingException && shouldRetry.test(((MessagingException) e).getEvent()));
  }

  private Function<Throwable, Throwable> getThrowableFunction(Event event) {
    return throwable -> {
      Throwable cause = getMessagingExceptionCause(throwable);
      return new MessagingException(event,
                                    new RetryPolicyExhaustedException(createStaticMessage(UNTIL_SUCCESSFUL_MSG_PREFIX,
                                                                                          cause.getMessage()),
                                                                      cause, this),
                                    this);
    };
  }

  private ReactiveProcessor scheduleRoute(ReactiveProcessor route) {
    if (flowConstruct instanceof Pipeline) {
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

  private static class FailureExpressionAssertionException extends MuleRuntimeException {

    public FailureExpressionAssertionException(I18nMessage message) {
      super(message);
    }
  }
}


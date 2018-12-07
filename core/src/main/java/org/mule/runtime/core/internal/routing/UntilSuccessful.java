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
import static org.mule.runtime.core.api.util.ExceptionUtils.getMessagingExceptionCause;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getProcessingStrategy;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChain;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContext;
import static reactor.core.publisher.Flux.from;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.AbstractMuleObjectOwner;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.policy.NoRetryPolicyTemplate;
import org.mule.runtime.core.api.retry.policy.RetryPolicyExhaustedException;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.privileged.processor.Scope;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * UntilSuccessful attempts to route a message to the message processor it contains. Routing is considered successful if no
 * exception has been raised and, optionally, if the response matches an expression.
 */
public class UntilSuccessful extends AbstractMuleObjectOwner implements Scope {

  private static final String UNTIL_SUCCESSFUL_MSG_PREFIX =
      "'until-successful' retries exhausted. Last exception message was: %s";
  private static final long DEFAULT_MILLIS_BETWEEN_RETRIES = 60 * 1000;
  private static final int DEFAULT_RETRIES = 5;

  @Inject
  private SchedulerService schedulerService;

  private int maxRetries = DEFAULT_RETRIES;
  private Long millisBetweenRetries = DEFAULT_MILLIS_BETWEEN_RETRIES;
  private MessageProcessorChain nestedChain;
  private Predicate<CoreEvent> shouldRetry;
  private RetryPolicyTemplate policyTemplate;
  private Scheduler timer;
  private List<Processor> processors;

  @Override
  public void initialise() throws InitialisationException {
    System.out.println("Initializing UntilSuccessful " + this);
    if (processors == null) {
      throw new InitialisationException(createStaticMessage("One message processor must be configured within 'until-successful'."),
                                        this);
    }
    this.nestedChain = newChain(getProcessingStrategy(locator, getRootContainerLocation()), processors);
    super.initialise();
    timer = schedulerService.cpuLightScheduler();
    policyTemplate =
        maxRetries != 0 ? new SimpleRetryPolicyTemplate(millisBetweenRetries, maxRetries) : new NoRetryPolicyTemplate();
    shouldRetry = event -> event.getError().isPresent();
  }

  @Override
  public void dispose() {
    System.out.println("Disposing UntilSuccessful " + this);
    super.dispose();
    timer.stop();
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return from(publisher)
        .flatMap(event -> Mono.from(processWithChildContext(event, nestedChain, ofNullable(getLocation())))
            .transform(p -> policyTemplate.applyPolicy(p, getRetryPredicate(), e -> {
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


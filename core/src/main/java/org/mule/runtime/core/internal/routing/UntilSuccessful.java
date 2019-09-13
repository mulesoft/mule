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
import static org.mule.runtime.core.api.util.ExceptionUtils.getMessagingExceptionCause;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.buildNewChainWithListOfProcessors;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getProcessingStrategy;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContextDontComplete;
import static reactor.core.publisher.Flux.from;

import com.github.benmanes.caffeine.cache.LoadingCache;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
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
import java.util.Optional;
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
        policyTemplate = of(new SimpleRetryPolicyTemplate(millisBetweenRetries, maxRetries));
      }
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
    Integer maxRetries =
        (Integer) expressionManager.evaluate(this.maxRetries, DataType.NUMBER, NULL_BINDING_CONTEXT, event).getValue();
    Integer millisBetweenRetries =
        (Integer) expressionManager.evaluate(this.millisBetweenRetries, DataType.NUMBER, NULL_BINDING_CONTEXT, event).getValue();
    return this.policyTemplatesCache.get(new Pair<>(millisBetweenRetries, maxRetries));
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return from(publisher)
        .flatMap(event -> Mono.from(processWithChildContextDontComplete(event, nestedChain, ofNullable(getLocation())))
            .transform(p -> policyTemplate.orElseGet(() -> createRetryPolicyTemplate(event)).applyPolicy(p, getRetryPredicate(),
                                                                                                         e -> {
                                                                                                         },
                                                                                                         getThrowableFunction(event),
                                                                                                         timer)));
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
   *
   * @param maxRetries the number of retries to process the route when failing. Default value is 5.
   */
  public void setMaxRetries(final int maxRetries) {
    this.maxRetries = String.valueOf(maxRetries);
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
   * @param millisBetweenRetries the number of milliseconds between retries. Default value is 60000.
   */
  public void setMillisBetweenRetries(Long millisBetweenRetries) {
    this.millisBetweenRetries = String.valueOf(millisBetweenRetries);
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


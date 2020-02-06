/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.Collections.singletonList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.buildNewChainWithListOfProcessors;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getProcessingStrategy;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.processor.AbstractMuleObjectOwner;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.routing.UntilSuccessfulRouter.RetryContextInitializationException;
import org.mule.runtime.core.privileged.processor.Scope;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import java.util.List;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.reactivestreams.Publisher;

/**
 * UntilSuccessful attempts to route a message to the message processor it contains. Routing is considered successful if no
 * exception has been raised and, optionally, if the response matches an expression.
 */
public class UntilSuccessful extends AbstractMuleObjectOwner implements Scope {

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
    shouldRetry = event -> event.getError().isPresent();
  }

  @Override
  public void dispose() {
    super.dispose();
    timer.stop();
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    try {
      return processToApply(event, this);
    } catch (Exception error) {
      Throwable cause = error.getCause();
      if (cause != null && cause instanceof RetryContextInitializationException &&
          cause.getCause() instanceof ExpressionRuntimeException) {
        // Runtime exception caused by Retry Ctx initialization, propagating
        throw ((ExpressionRuntimeException) cause.getCause());
      } else {
        // Not caused by context initialization. Throwing as raised.
        throw error;
      }
    }
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return new UntilSuccessfulRouter(this, publisher, nestedChain, expressionManager, shouldRetry, timer, maxRetries,
                                     millisBetweenRetries).getDownstreamPublisher();
  }


  /**
   * @return the number of times the scope will retry before failing. Default value is 5.
   */
  public String getMaxRetries() {
    return maxRetries;
  }

  /**
   *
   * @param maxRetries the number of times the scope will retry before failing. Default value is 5.
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


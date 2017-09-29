/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.routing;

import static org.mule.runtime.api.el.BindingContextUtils.getTargetBindingContext;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.event.CoreEvent.builder;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.TIMEOUT;
import static org.mule.runtime.core.internal.processor.strategy.DirectProcessingStrategyFactory.DIRECT_PROCESSING_STRATEGY_INSTANCE;
import static org.mule.runtime.core.internal.component.ComponentUtils.getFromAnnotatedObject;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static reactor.core.publisher.Flux.from;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.AbstractMuleObjectOwner;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.privileged.processor.Router;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.privileged.routing.CompositeRoutingException;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.routing.ForkJoinStrategy.RoutingPair;
import org.mule.runtime.api.scheduler.SchedulerService;

import java.util.function.Consumer;

import javax.inject.Inject;

import org.reactivestreams.Publisher;

/**
 * Abstract base class for routers using a {@link ForkJoinStrategy} to process multiple {@link RoutingPair}'s and aggregate
 * results.
 * 
 * @since 4.0
 */
public abstract class AbstractForkJoinRouter extends AbstractMuleObjectOwner<MessageProcessorChain> implements Router {

  @Inject
  private SchedulerService schedulerService;

  @Inject
  private ConfigurationComponentLocator componentLocator;

  private ProcessingStrategy processingStrategy;
  private ForkJoinStrategyFactory forkJoinStrategyFactory;
  private ForkJoinStrategy forkJoinStrategy;
  private long timeout = Long.MAX_VALUE;
  private Integer maxConcurrency;
  private Scheduler timeoutScheduler;
  private ErrorType timeoutErrorType;
  private ExtendedExpressionManager expressionManager;
  private String target;
  private String targetValue = "#[payload]";

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return from(publisher)
        .doOnNext(onEvent())
        .flatMap(event -> from(forkJoinStrategy.forkJoin(event, getRoutingPairs(event)))
            .map(result -> {
              if (target != null) {
                TypedValue targetValue = getTargetValue(result);
                return builder(event).addVariable(target, targetValue.getValue(), targetValue.getDataType()).build();
              } else {
                return result;
              }
            })
            // Required due to lack of decent support for error-handling in reactor. See
            // https://github.com/reactor/reactor-core/issues/629.
            .onErrorMap(throwable -> !(throwable instanceof MessagingException),
                        throwable -> new MessagingException(event, throwable, this)));
  }

  /**
   * Template method to perform any operation using the original event before processing.
   *
   * @return event function to apply to incoming {@link CoreEvent}
   */
  protected Consumer<CoreEvent> onEvent() {
    return event -> {
    };
  }

  /**
   * Returns a list of {@link RoutingPair}'s to be processed by the {@link ForkJoinStrategy}.
   * 
   * @param event the incoming event in the route.
   * @return a potentially non-finite
   */
  protected abstract Publisher<RoutingPair> getRoutingPairs(CoreEvent event);

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    processingStrategy = getFromAnnotatedObject(componentLocator, this)
        .map(flow -> flow.getProcessingStrategy())
        .orElse(DIRECT_PROCESSING_STRATEGY_INSTANCE);
    expressionManager = muleContext.getExpressionManager();
    timeoutScheduler = schedulerService.cpuLightScheduler();
    timeoutErrorType = muleContext.getErrorTypeRepository().getErrorType(TIMEOUT).get();
    maxConcurrency = maxConcurrency != null ? maxConcurrency : getDefaultMaxConcurrency();
    forkJoinStrategyFactory = forkJoinStrategyFactory != null ? forkJoinStrategyFactory : getDefaultForkJoinStrategyFactory();

    forkJoinStrategy =
        forkJoinStrategyFactory.createForkJoinStrategy(processingStrategy, maxConcurrency,
                                                       isDelayErrors(), timeout, timeoutScheduler, timeoutErrorType);
  }

  @Override
  public void dispose() {
    if (timeoutScheduler != null) {
      timeoutScheduler.stop();
    }
    super.dispose();
  }

  /**
   * Set the {@link ForkJoinStrategyFactory} to use for this router. This defines how routing pairs are processed and how results
   * are aggregated to create a single result event.
   * 
   * @param forkJoinStrategyFactory
   */
  public void setForkJoinStrategyFactory(ForkJoinStrategyFactory forkJoinStrategyFactory) {
    this.forkJoinStrategyFactory = forkJoinStrategyFactory;
  }

  /**
   * Set the timeout applied to each routing pair.
   * <p>
   * Where maxConcurrency > # routing pairs this is also the effective timeout for the router, but when maxConcurrency < # routing
   * pairs then the effective timeout for the router will be higher.
   *
   * @param timeout timeout in ms
   * @throws IllegalArgumentException if the value is zero or less.
   */
  public void setTimeout(long timeout) {
    checkArgument(timeout > 0, "Timeout must be greater than zero");
    this.timeout = timeout;
  }

  /**
   * Set the maximum concurrency which defines at most how any routing pairs can execute in parallel.
   * 
   * @param maxConcurrency
   * @throws IllegalArgumentException if the value is zero or less.
   */
  public void setMaxConcurrency(int maxConcurrency) {
    checkArgument(timeout > 0, "Maximum concurrency must be one or more.");
    this.maxConcurrency = maxConcurrency;
  }

  /**
   * The variable where the result from this router should be stored. If this is not set then the result is set in the payload.
   * 
   * @param target a variable name.
   */
  public void setTarget(String target) {
    this.target = target;
  }

  /**
   * Defines the target value expression
   *
   * @param targetValue the target value expresion
   */
  public void setTargetValue(String targetValue) {
    this.targetValue = targetValue;
  }

  /**
   * Template method that allows implementations to define a default max concurrency.
   * 
   * @return
   */
  protected abstract int getDefaultMaxConcurrency();

  /**
   * Template method that allows implementations to define if the errors should be delayed, all routing pairs executed and a
   * {@link CompositeRoutingException}
   *
   * @return the default value of delay errors.
   */
  protected abstract boolean isDelayErrors();

  /**
   * Template method that allows implementations to the default {@link ForkJoinStrategyFactory} that should be used if one isn't
   * configured.
   * 
   * @return the default fork-join strategy.
   */
  protected abstract ForkJoinStrategyFactory getDefaultForkJoinStrategyFactory();

  private TypedValue getTargetValue(CoreEvent event) {
    return muleContext.getExpressionManager().evaluate(targetValue, getTargetBindingContext(event.getMessage()));
  }

}

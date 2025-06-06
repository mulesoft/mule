/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.construct;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.construct.Flow.INITIAL_STATE_STARTED;
import static org.mule.runtime.core.internal.construct.AbstractFlowConstruct.createFlowStatistics;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.deployment.management.ComponentInitialStateManager;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.Flow.Builder;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.management.stats.AllStatistics;
import org.mule.runtime.core.api.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.exception.GlobalErrorHandler;
import org.mule.runtime.core.internal.management.stats.DefaultFlowsSummaryStatistics;
import org.mule.runtime.core.internal.processor.strategy.DirectProcessingStrategyFactory;
import org.mule.runtime.core.privileged.processor.MessageProcessors;

import java.util.List;
import java.util.Optional;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;

/**
 * Creates instances of {@link Flow} with a default implementation
 * <p>
 * Builder instance can be configured using the methods that follow the builder pattern until the flow is built. After that point,
 * builder methods will fail to update the builder state.
 */
public class DefaultFlowBuilder implements Builder {

  private final String name;
  private final MuleContext muleContext;
  private final ComponentInitialStateManager componentInitialStateManager;
  private MessageSource source;
  private List<Processor> processors = emptyList();
  private FlowExceptionHandler exceptionListener;
  private ProcessingStrategyFactory processingStrategyFactory;
  private String initialState = INITIAL_STATE_STARTED;
  private Integer maxConcurrency;

  private DefaultFlow flow;

  /**
   * Creates a new builder
   *
   * @param name                         name of the flow to be created. Non empty.
   * @param muleContext                  context where the flow will be associated with. Non null.
   * @param componentInitialStateManager component state manager used by the flow to determine what components must be started or
   *                                     not. Not null.
   */
  public DefaultFlowBuilder(String name, MuleContext muleContext, ComponentInitialStateManager componentInitialStateManager) {
    checkArgument(isNotEmpty(name), "name cannot be empty");
    checkArgument(muleContext != null, "muleContext cannot be null");
    checkArgument(componentInitialStateManager != null, "componentInitialStateManager cannot be null");

    this.componentInitialStateManager = componentInitialStateManager;
    this.name = name;
    this.muleContext = muleContext;
  }

  /**
   * Configures the message source for the flow.
   *
   * @param source source of messages to use. Non null.
   * @return same builder instance.
   */
  @Override
  public Builder source(MessageSource source) {
    checkImmutable();
    checkArgument(source != null, "source cannot be null");
    this.source = source;

    return this;
  }

  /**
   * Configures the message processors to execute as part of flow.
   *
   * @param processors processors to execute on a {@link Message}. Non null.
   * @return same builder instance.
   */
  @Override
  public Builder processors(List<Processor> processors) {
    checkImmutable();
    checkArgument(processors != null, "processors cannot be null");
    this.processors = processors;

    return this;
  }

  /**
   * Configures the message processors to execute as part of flow.
   *
   * @param processors processors to execute on a {@link Message}.
   * @return same builder instance.
   */
  @Override
  public Builder processors(Processor... processors) {
    checkImmutable();
    this.processors = asList(processors);

    return this;
  }

  /**
   * Configures the exception listener to manage exceptions thrown on the flow execution.
   *
   * @param exceptionListener exception listener to use on the flow.
   * @return same builder instance
   */
  @Override
  public Builder messagingExceptionHandler(FlowExceptionHandler exceptionListener) {
    checkImmutable();
    this.exceptionListener = exceptionListener;
    return this;
  }

  /**
   * Configures the factory used to create processing strategies on the created flow.
   *
   * @param processingStrategyFactory factory to create processing strategies. Non null.
   * @return same builder instance.
   */
  @Override
  public Builder processingStrategyFactory(ProcessingStrategyFactory processingStrategyFactory) {
    checkImmutable();
    checkArgument(processingStrategyFactory != null, "processingStrategyFactory cannot be null");
    this.processingStrategyFactory = processingStrategyFactory;
    return this;
  }

  @Override
  public Builder withDirectProcessingStrategyFactory() {
    return processingStrategyFactory(new DirectProcessingStrategyFactory());
  }

  @Override
  public Builder initialState(String initialState) {
    checkImmutable();
    checkArgument(initialState != null, "initialState cannot be null");
    this.initialState = initialState;
    return this;
  }

  @Override
  public Builder maxConcurrency(int maxConcurrency) {
    checkImmutable();
    checkArgument(maxConcurrency > 0, "maxConcurrency cannot be less than 1");
    this.maxConcurrency = maxConcurrency;
    return this;
  }

  /**
   * Builds a flow with the provided configuration.
   *
   * @return a new flow instance.
   */
  @Override
  public Flow build() {
    checkImmutable();

    AllStatistics statistics = muleContext.getStatistics();

    flow = new DefaultFlow(name, muleContext, source, processors,
                           ofNullable(exceptionListener), ofNullable(processingStrategyFactory), initialState, maxConcurrency,
                           (DefaultFlowsSummaryStatistics) statistics.getFlowSummaryStatistics(),
                           (DefaultFlowsSummaryStatistics) statistics.getFlowSummaryStatisticsV2(),
                           createFlowStatistics(name, statistics),
                           componentInitialStateManager);

    return flow;
  }

  protected final void checkImmutable() {
    if (flow != null) {
      throw new IllegalStateException("Cannot change attributes once the flow was built");
    }
  }

  /**
   * Default implementation of {@link Flow}
   */
  public static class DefaultFlow extends AbstractPipeline implements Flow {

    protected DefaultFlow(String name, MuleContext muleContext, MessageSource source, List<Processor> processors,
                          Optional<FlowExceptionHandler> exceptionListener,
                          Optional<ProcessingStrategyFactory> processingStrategyFactory, String initialState,
                          Integer maxConcurrency,
                          DefaultFlowsSummaryStatistics flowsSummaryStatistics,
                          DefaultFlowsSummaryStatistics flowsSummaryStatisticsV2,
                          FlowConstructStatistics flowConstructStatistics,
                          ComponentInitialStateManager componentInitialStateManager) {
      super(name, muleContext, source, processors, exceptionListener, processingStrategyFactory, initialState, maxConcurrency,
            flowsSummaryStatistics, flowsSummaryStatisticsV2, flowConstructStatistics, componentInitialStateManager);

      if (this.getExceptionListener() instanceof GlobalErrorHandler) {
        ((GlobalErrorHandler) this.getExceptionListener()).addComponentReference(getName());
      }
    }

    @Override
    public CoreEvent process(final CoreEvent event) throws MuleException {
      return processToApply(event, this);
    }

    @Override
    public ReactiveProcessor referenced() {
      return pub -> from(pub)
          .doOnNext(this::checkBackpressureReferenced)
          .transform(dispatchToFlow());
    }

    /**
     * This implementation does not support {@link Flux}es, but because of backwards compatibility we cannot "improve" it.
     */
    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
      return from(publisher)
          .doOnNext(assertStarted())
          // Insert the incoming event into the flow, routing it through the processing strategy
          .transformDeferred(routeThroughProcessingStrategyTransformer())
          // Don't handle errors, these will be handled by parent flow
          .onErrorStop();
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link ProcessingStrategyFactory}
     */
    @Override
    protected ProcessingStrategyFactory createDefaultProcessingStrategyFactory() {
      return MessageProcessors.createDefaultProcessingStrategyFactory();
    }

    @Override
    public boolean isSynchronous() {
      return getProcessingStrategy() != null ? getProcessingStrategy().isSynchronous() : true;
    }
  }
}

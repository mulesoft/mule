/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.construct;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.config.MuleProperties.COMPATIBILITY_PLUGIN_INSTALLED;
import static org.mule.runtime.core.api.construct.Flow.INITIAL_STATE_STARTED;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.api.processor.strategy.AsyncProcessingStrategyFactory.DEFAULT_MAX_CONCURRENCY;
import static org.mule.runtime.core.internal.construct.AbstractFlowConstruct.createFlowStatistics;
import static org.mule.runtime.core.internal.event.DefaultEventContext.child;
import static org.mule.runtime.core.privileged.event.PrivilegedEvent.setCurrentEvent;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.Flow.Builder;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.processor.strategy.DirectProcessingStrategyFactory;
import org.mule.runtime.core.internal.processor.strategy.TransactionAwareWorkQueueProcessingStrategyFactory;
import org.mule.runtime.core.internal.routing.requestreply.SimpleAsyncRequestReplyRequester.AsyncReplyToPropertyRequestReplyReplier;
import org.mule.runtime.core.internal.util.MessagingExceptionResolver;
import org.mule.runtime.core.privileged.endpoint.LegacyImmutableEndpoint;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;
import org.mule.runtime.core.privileged.exception.AbstractExceptionListener;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChainBuilder;

import org.reactivestreams.Publisher;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;

import reactor.core.publisher.Mono;

/**
 * Creates instances of {@link Flow} with a default implementation
 *
 * <p/>
 * Builder instance can be configured using the methods that follow the builder pattern until the flow is built. After that point,
 * builder methods will fail to update the builder state.
 */
public class DefaultFlowBuilder implements Builder {

  private final String name;
  private final MuleContext muleContext;

  private MessageSource source;
  private List<Processor> processors = emptyList();
  private FlowExceptionHandler exceptionListener;
  private ProcessingStrategyFactory processingStrategyFactory;
  private String initialState = INITIAL_STATE_STARTED;
  private int maxConcurrency = DEFAULT_MAX_CONCURRENCY;

  private DefaultFlow flow;

  /**
   * Creates a new builder
   *
   * @param name name of the flow to be created. Non empty.
   * @param muleContext context where the flow will be associated with. Non null.
   */
  public DefaultFlowBuilder(String name, MuleContext muleContext) {
    checkArgument(isNotEmpty(name), "name cannot be empty");
    checkArgument(muleContext != null, "muleContext cannot be null");

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

    FlowConstructStatistics flowStatistics = createFlowStatistics(name, muleContext);
    if (exceptionListener instanceof AbstractExceptionListener) {
      ((AbstractExceptionListener) exceptionListener).setStatistics(flowStatistics);
    }
    flow = new DefaultFlow(name, muleContext, source, processors,
                           ofNullable(exceptionListener), ofNullable(processingStrategyFactory), initialState, maxConcurrency,
                           flowStatistics);

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

    private final MessagingExceptionResolver exceptionResolver = new MessagingExceptionResolver(this);

    private boolean handleReplyTo = false;

    protected DefaultFlow(String name, MuleContext muleContext, MessageSource source, List<Processor> processors,
                          Optional<FlowExceptionHandler> exceptionListener,
                          Optional<ProcessingStrategyFactory> processingStrategyFactory, String initialState,
                          int maxConcurrency, FlowConstructStatistics flowConstructStatistics) {
      super(name, muleContext, source, processors, exceptionListener, processingStrategyFactory, initialState, maxConcurrency,
            flowConstructStatistics);
    }

    @Override
    protected void doInitialise() throws MuleException {
      super.doInitialise();

      if (((MuleContextWithRegistries) muleContext).getRegistry().lookupObject(COMPATIBILITY_PLUGIN_INSTALLED) != null) {
        handleReplyTo = true;
      }
    }

    @Override
    public CoreEvent process(final CoreEvent event) throws MuleException {
      return processToApply(event, this);
    }

    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
      return from(publisher)
          .doOnNext(assertStarted())
          .flatMap(event -> {
            CoreEvent request = createMuleEventForCurrentFlow((PrivilegedEvent) event);
            // Use sink and potentially shared stream in Flow by dispatching incoming event via sink and then using
            // response publisher to operate of the result of flow processing before returning
            try {
              getSink().accept(request);
            } catch (RejectedExecutionException ree) {
              MessagingException me = new MessagingException(event, ree, this);
              ((BaseEventContext) request.getContext()).error(exceptionResolver.resolve(me, getMuleContext()));
            }
            return Mono.from(((BaseEventContext) request.getContext()).getResponsePublisher())
                .cast(PrivilegedEvent.class)
                .map(r -> {
                  CoreEvent result = createReturnEventForParentFlowConstruct(r, (InternalEvent) event);
                  return result;
                })
                .onErrorMap(MessagingException.class, me -> {
                  me.setProcessedEvent(createReturnEventForParentFlowConstruct((PrivilegedEvent) me.getEvent(),
                                                                               (InternalEvent) event));
                  return me;
                });
          });
    }

    private PrivilegedEvent createMuleEventForCurrentFlow(PrivilegedEvent event) {
      if (handleReplyTo) {
        // Create new event with replyToHandler etc.
        event = InternalEvent.builder(event)
            // DefaultReplyToHandler is used differently and should only be invoked by the first flow and not any
            // referenced flows. If it is passed on they two replyTo responses are sent.
            .replyToHandler(null)
            .replyToDestination(event.getReplyToDestination()).build();
        // Update RequestContext ThreadLocal for backwards compatibility
        setCurrentEvent(event);
      }
      return event;
    }

    private PrivilegedEvent createReturnEventForParentFlowConstruct(PrivilegedEvent result, InternalEvent original) {
      if (handleReplyTo) {
        if (result != null) {
          // Create new event with ReplyToHandler and synchronous
          result = InternalEvent.builder(result)
              .replyToHandler(original.getReplyToHandler())
              .replyToDestination(original.getReplyToDestination())
              .build();
        }
        // Update RequestContext ThreadLocal for backwards compatibility
        setCurrentEvent(result);
      }
      return result;
    }

    @Override
    protected void configureMessageProcessors(MessageProcessorChainBuilder builder) throws MuleException {
      super.configureMessageProcessors(builder);
      if (getSource() instanceof LegacyImmutableEndpoint
          && !((LegacyImmutableEndpoint) getSource()).getExchangePattern().hasResponse()) {
        builder.chain(new AsyncReplyToPropertyRequestReplyReplier(getSource()));
      }
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link ProcessingStrategyFactory}
     */
    @Override
    protected ProcessingStrategyFactory createDefaultProcessingStrategyFactory() {
      return new TransactionAwareWorkQueueProcessingStrategyFactory();
    }

    @Override
    public String getConstructType() {
      return "Flow";
    }

    @Override
    public boolean isSynchronous() {
      return getProcessingStrategy() != null ? getProcessingStrategy().isSynchronous() : true;
    }

    @Override
    protected EventContext createEventContext(Publisher<Void> externalCompletionPublisher) {
      return create(this, getLocation(), null, externalCompletionPublisher);
    }

    @Override
    protected BaseEventContext createChildEventContext(EventContext parent) {
      return child((BaseEventContext) parent, ofNullable(getLocation()), getExceptionListener());
    }
  }
}

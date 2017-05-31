/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.construct;

import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.Event.setCurrentEvent;
import static org.mule.runtime.core.api.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.execution.ErrorHandlingExecutionTemplate.createErrorHandlingExecutionTemplate;
import static org.mule.runtime.core.util.ExceptionUtils.updateMessagingExceptionWithError;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.Flow.Builder;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.interceptor.ProcessingTimeInterceptor;
import org.mule.runtime.core.internal.construct.processor.FlowConstructStatisticsMessageProcessor;
import org.mule.runtime.core.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.processor.strategy.DefaultFlowProcessingStrategyFactory;
import org.mule.runtime.core.routing.requestreply.AsyncReplyToPropertyRequestReplyReplier;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.RejectedExecutionException;

import org.reactivestreams.Publisher;

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
  private MessageSource messageSource;
  private List<Processor> messageProcessors;
  private MessagingExceptionHandler exceptionListener;
  private ProcessingStrategyFactory processingStrategyFactory;
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
   * @param messageSource message source to use. Non null.
   * @return same builder instance.
   */
  @Override
  public Builder messageSource(MessageSource messageSource) {
    checkImmutable();
    checkArgument(messageSource != null, "messageSource cannot be null");
    this.messageSource = messageSource;

    return this;
  }

  /**
   * Configures the message processors to execute as part of flow.
   *
   * @param messageProcessors message processors to execute. Non null.
   * @return same builder instance.
   */
  @Override
  public Builder messageProcessors(List<Processor> messageProcessors) {
    checkImmutable();
    checkArgument(messageProcessors != null, "messageProcessors cannot be null");
    this.messageProcessors = messageProcessors;

    return this;
  }

  /**
   * Configures the exception listener to manage exceptions thrown on the flow execution.
   *
   * @param exceptionListener exception listener to use on the flow.
   * @return same builder instance
   */
  @Override
  public Builder messagingExceptionHandler(MessagingExceptionHandler exceptionListener) {
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

  /**
   * Builds a flow with the provided configuration.
   *
   * @return a new flow instance.
   */
  @Override
  public Flow build() {
    checkImmutable();

    flow = new DefaultFlow(name, muleContext);
    if (messageSource != null) {
      flow.setMessageSource(messageSource);
    }

    if (messageProcessors != null) {
      flow.setMessageProcessors(messageProcessors);
    }

    if (exceptionListener != null) {
      flow.setExceptionListener(exceptionListener);
    }

    if (processingStrategyFactory != null) {
      flow.setProcessingStrategyFactory(processingStrategyFactory);
    }

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

    protected DefaultFlow(String name, MuleContext muleContext) {
      super(name, muleContext);
    }

    @Override
    public Event process(final Event event) throws MuleException {
      if (useBlockingCodePath()) {
        return processBlockingSynchronous(event);
      } else {
        return processToApply(event, this);
      }
    }

    /*
     * Process flow using blocking {@link Processor#process(Event)} methods synchronously without apply any processing strategy.
     * Note that individual processors internally may still you asynchronous non-blocking behaviour to achieved required
     * functionality.
     */
    private Event processBlockingSynchronous(Event event) throws MessagingException, DefaultMuleException {
      // TODO MULE-11023 Migrate transaction execution template mechanism to use non-blocking API
      final Event newEvent = createMuleEventForCurrentFlow(event, event.getReplyToDestination(), event.getReplyToHandler());
      try {
        ExecutionTemplate<Event> executionTemplate =
            createErrorHandlingExecutionTemplate(muleContext, this, getExceptionListener());
        Event result = executionTemplate.execute(() -> pipeline.process(newEvent));
        newEvent.getContext().success(result);
        return createReturnEventForParentFlowConstruct(result, event);
      } catch (MessagingException e) {
        e.setProcessedEvent(createReturnEventForParentFlowConstruct(e.getEvent(), event));
        newEvent.getContext().error(e);
        throw e;
      } catch (Exception e) {
        newEvent.getContext().error(e);
        resetRequestContextEvent(event);
        throw new DefaultMuleException(CoreMessages.createStaticMessage("Flow execution exception"), e);
      }
    }

    @Override
    public Publisher<Event> apply(Publisher<Event> publisher) {
      return from(publisher)
          .doOnNext(assertStarted())
          .flatMap(event -> {
            Event request = createMuleEventForCurrentFlow(event, event.getReplyToDestination(), event.getReplyToHandler());
            // Use sink and potentially shared stream in Flow by dispatching incoming event via sink and then using
            // response publisher to operate of the result of flow processing before returning
            try {
              sink.accept(request);
            } catch (RejectedExecutionException ree) {
              request.getContext()
                  .error(updateMessagingExceptionWithError(new MessagingException(event, ree, this), this, this));
            }
            return Mono.from(request.getContext().getResponsePublisher())
                .map(r -> {
                  Event result = createReturnEventForParentFlowConstruct(r, event);
                  return result;
                })
                .onErrorMap(MessagingException.class, me -> {
                  me.setProcessedEvent(createReturnEventForParentFlowConstruct(me.getEvent(), event));
                  return me;
                });
          });
    }

    private Event createMuleEventForCurrentFlow(Event event, Object replyToDestination, ReplyToHandler replyToHandler) {
      // DefaultReplyToHandler is used differently and should only be invoked by the first flow and not any
      // referenced flows. If it is passed on they two replyTo responses are sent.
      replyToHandler = null;

      // TODO MULE-10013
      // Create new event for current flow with current flowConstruct, replyToHandler etc.
      event = Event.builder(event).flow(this).replyToHandler(replyToHandler).replyToDestination(replyToDestination).build();
      resetRequestContextEvent(event);
      return event;
    }

    private Event createReturnEventForParentFlowConstruct(Event result, Event original) {
      if (result != null) {
        Optional<Error> errorOptional = result.getError();
        // TODO MULE-10013
        // Create new event with original FlowConstruct, ReplyToHandler and synchronous
        result = Event.builder(result).flow(original.getFlowConstruct())
            .replyToHandler(original.getReplyToHandler())
            .replyToDestination(original.getReplyToDestination())
            .error(errorOptional.orElse(null)).build();
      }
      resetRequestContextEvent(result);
      return result;
    }

    private void resetRequestContextEvent(Event event) {
      // Update RequestContext ThreadLocal for backwards compatibility
      setCurrentEvent(event);
    }

    @Override
    protected void configurePreProcessors(MessageProcessorChainBuilder builder) throws MuleException {
      super.configurePreProcessors(builder);
      builder.chain(new ProcessingTimeInterceptor());
      builder.chain(new FlowConstructStatisticsMessageProcessor());
    }

    @Override
    protected void configurePostProcessors(MessageProcessorChainBuilder builder) throws MuleException {
      builder.chain(new AsyncReplyToPropertyRequestReplyReplier());
      super.configurePostProcessors(builder);
    }

    /**
     * {@inheritDoc}
     *
     * @return a {@link DefaultFlowProcessingStrategyFactory}
     */
    @Override
    protected ProcessingStrategyFactory createDefaultProcessingStrategyFactory() {
      return new DefaultFlowProcessingStrategyFactory();
    }

    @Override
    public String getConstructType() {
      return "Flow";
    }

    @Override
    protected void configureStatistics() {
      statistics = new FlowConstructStatistics(getConstructType(), name);
      statistics.setEnabled(muleContext.getStatistics().isEnabled());
      muleContext.getStatistics().add(statistics);
    }

    @Override
    public boolean isSynchronous() {
      return getProcessingStrategy() != null ? getProcessingStrategy().isSynchronous() : true;
    }

  }
}

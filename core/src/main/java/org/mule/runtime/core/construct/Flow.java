/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.construct;

import static org.mule.runtime.core.api.Event.setCurrentEvent;
import static org.mule.runtime.core.api.rx.Exceptions.rxExceptionToMuleException;
import static org.mule.runtime.core.execution.ErrorHandlingExecutionTemplate.createErrorHandlingExecutionTemplate;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.api.processor.DynamicPipeline;
import org.mule.runtime.core.api.processor.DynamicPipelineBuilder;
import org.mule.runtime.core.api.processor.DynamicPipelineException;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.construct.processor.FlowConstructStatisticsMessageProcessor;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.interceptor.ProcessingTimeInterceptor;
import org.mule.runtime.core.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.processor.strategy.DefaultFlowProcessingStrategyFactory;
import org.mule.runtime.core.routing.requestreply.AsyncReplyToPropertyRequestReplyReplier;

import java.util.Optional;

import org.reactivestreams.Publisher;

/**
 * This implementation of {@link AbstractPipeline} adds the following functionality:
 * <ul>
 * <li>Rejects inbound events when Flow is not started</li>
 * <li>Gathers statistics and processing time data</li>
 * <li>Implements MessagePorcessor allowing direct invocation of the pipeline</li>
 * <li>Supports the optional configuration of a {@link ProcessingStrategy} that determines how message processors are processed.
 * </ul>
 */
public class Flow extends AbstractPipeline implements Processor, DynamicPipeline {

  private DynamicPipelineMessageProcessor dynamicPipelineMessageProcessor;

  public Flow(String name, MuleContext muleContext) {
    super(name, muleContext);
  }

  @Override
  public Event process(final Event event) throws MuleException {
    if (useBlockingCodePath()) {
      // TODO MULE-11023 Migrate transaction execution template mechanism to use non-blocking API
      final Event newEvent = createMuleEventForCurrentFlow(event, event.getReplyToDestination(), event.getReplyToHandler());
      try {
        ExecutionTemplate<Event> executionTemplate =
            createErrorHandlingExecutionTemplate(muleContext, this, getExceptionListener());
        Event result = executionTemplate.execute(() -> pipeline.process(newEvent));
        return createReturnEventForParentFlowConstruct(result, event);
      } catch (MessagingException e) {
        e.setProcessedEvent(createReturnEventForParentFlowConstruct(e.getEvent(), event));
        throw e;
      } catch (Exception e) {
        resetRequestContextEvent(event);
        throw new DefaultMuleException(CoreMessages.createStaticMessage("Flow execution exception"), e);
      }
    } else {
      try {
        return just(event).transform(this).block();
      } catch (Exception e) {
        throw rxExceptionToMuleException(e);
      }
    }
  }

  @Override
  public Publisher<Event> apply(Publisher<Event> publisher) {
    return from(publisher).concatMap(event -> just(event)
        .map(request -> createMuleEventForCurrentFlow(request, request.getReplyToDestination(), request.getReplyToHandler()))
        .transform(processFlowFunction())
        .map(response -> createReturnEventForParentFlowConstruct(response, event)));
  }

  private Event createMuleEventForCurrentFlow(Event event, Object replyToDestination, ReplyToHandler replyToHandler) {
    // DefaultReplyToHandler is used differently and should only be invoked by the first flow and not any
    // referenced flows. If it is passded on they two replyTo responses are sent.
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
      result = Event.builder(result).flow(original.getFlowConstruct()).replyToHandler(original.getReplyToHandler())
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
    builder.chain(new ProcessIfPipelineStartedMessageProcessor());
    builder.chain(new ProcessingTimeInterceptor());
    builder.chain(new FlowConstructStatisticsMessageProcessor());

    dynamicPipelineMessageProcessor = new DynamicPipelineMessageProcessor(this);
    builder.chain(dynamicPipelineMessageProcessor);
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
  public DynamicPipelineBuilder dynamicPipeline(String id) throws DynamicPipelineException {
    return dynamicPipelineMessageProcessor.dynamicPipeline(id);
  }

  @Override
  public boolean isSynchronous() {
    return getProcessingStrategy() != null ? getProcessingStrategy().isSynchronous() : true;
  }

}

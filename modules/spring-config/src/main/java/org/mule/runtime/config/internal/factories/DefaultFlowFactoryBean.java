/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.factories;

import static java.util.Collections.emptyList;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.Flow.Builder;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.construct.DefaultFlowBuilder.DefaultFlow;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.privileged.registry.RegistrationException;

import java.util.List;

import org.springframework.beans.factory.FactoryBean;

/**
 * Generates an immutable {@link Flow} with the provided config.
 *
 * @since 4.0
 */
public class DefaultFlowFactoryBean extends AbstractComponent implements FactoryBean<Flow> {

  private String name;
  private MuleContext muleContext;
  private String initialState;
  private MessageSource messageSource;
  private List<Processor> messageProcessors;
  private FlowExceptionHandler exceptionListener;
  private ProcessingStrategyFactory processingStrategyFactory;
  private Integer maxConcurrency;
  private FeatureFlaggingService featureFlaggingService;

  @Override
  public Flow getObject() throws Exception {
    Builder flowBuilder = Flow.builder(name, muleContext)
        .messagingExceptionHandler(exceptionListener)
        .initialState(initialState);

    flowBuilder.processors(messageProcessors != null
        ? messageProcessors
        : emptyList());

    if (messageSource != null) {
      flowBuilder.source(messageSource);
    }
    if (processingStrategyFactory != null) {
      flowBuilder.processingStrategyFactory(processingStrategyFactory);
    }
    if (maxConcurrency != null) {
      flowBuilder.maxConcurrency(maxConcurrency.intValue());
    }

    final DefaultFlow flow = (DefaultFlow) flowBuilder.build();

    flow.setAnnotations(getAnnotations());
    flow.setFeatureFlaggingService(featureFlaggingService);
    return flow;
  }

  @Override
  public Class<Flow> getObjectType() {
    return Flow.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }


  public String getName() {
    return name;
  }


  public void setName(String name) {
    this.name = name;
  }


  public MuleContext getMuleContext() {
    return muleContext;
  }


  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
    try {
      this.featureFlaggingService =
          ((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(FeatureFlaggingService.class);
    } catch (RegistrationException e) {
      throw new MuleRuntimeException(e);
    }
  }


  public String getInitialState() {
    return initialState;
  }


  public void setInitialState(String initialState) {
    this.initialState = initialState;
  }


  public MessageSource getMessageSource() {
    return messageSource;
  }


  public void setMessageSource(MessageSource messageSource) {
    this.messageSource = messageSource;
  }


  public List<Processor> getMessageProcessors() {
    return messageProcessors;
  }


  public void setMessageProcessors(List<Processor> messageProcessors) {
    this.messageProcessors = messageProcessors;
  }


  public FlowExceptionHandler getExceptionListener() {
    return exceptionListener;
  }


  public void setExceptionListener(FlowExceptionHandler exceptionListener) {
    this.exceptionListener = exceptionListener;
  }


  public ProcessingStrategyFactory getProcessingStrategyFactory() {
    return processingStrategyFactory;
  }


  public void setProcessingStrategyFactory(ProcessingStrategyFactory processingStrategyFactory) {
    this.processingStrategyFactory = processingStrategyFactory;
  }

  public void setMaxConcurrency(Integer maxConcurrency) {
    this.maxConcurrency = maxConcurrency;
  }

}

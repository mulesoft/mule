/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor.chain;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.privileged.processor.MessageProcessorBuilder;
import org.mule.runtime.tracer.api.component.ComponentTracer;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Constructs a chain of {@link Processor}s and wraps the invocation of the chain in a composite MessageProcessor. Both
 * MessageProcessors and InterceptingMessageProcessor's can be chained together arbitrarily in a single chain.
 * InterceptingMessageProcessors simply intercept the next MessageProcessor in the chain. When other non-intercepting
 * MessageProcessors are used an adapter is used internally to chain the MessageProcessor with the next in the chain.
 * </p>
 * <p>
 * The MessageProcessor instance that this builder builds can be nested in other chains as required.
 * </p>
 */
public abstract class AbstractMessageProcessorChainBuilder implements MessageProcessorChainBuilder {

  protected List<Processor> processors = new ArrayList<>();
  protected String name;
  protected ProcessingStrategy processingStrategy;
  protected FlowExceptionHandler messagingExceptionHandler;
  protected ComponentLocation location;
  protected MuleContext muleContext;
  protected ComponentTracer<CoreEvent> chainComponentTracer;

  // Argument is of type Object because it could be a MessageProcessor or a MessageProcessorBuilder
  protected Processor initializeMessageProcessor(Object processor) {
    if (processor instanceof MessageProcessorBuilder) {
      return ((MessageProcessorBuilder) processor).build();
    } else {
      return (Processor) processor;
    }
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public void setProcessingStrategy(ProcessingStrategy processingStrategy) {
    this.processingStrategy = processingStrategy;
  }

  public void setMessagingExceptionHandler(FlowExceptionHandler messagingExceptionHandler) {
    this.messagingExceptionHandler = messagingExceptionHandler;
  }

  public void setPipelineLocation(ComponentLocation location) {
    this.location = location;
  }

  @Override
  public void setComponentTracer(ComponentTracer<CoreEvent> chainComponentTracer) {
    this.chainComponentTracer = chainComponentTracer;
  }
}

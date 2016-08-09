/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf.config;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.NonBlockingSupported;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorBuilder;
import org.mule.runtime.core.api.processor.MessageProcessorContainer;
import org.mule.runtime.core.api.processor.MessageProcessorPathElement;

/**
 * Wraps a {@link MessageProcessorBuilder} and configures it lazily so it can be injected with the {@link FlowConstruct}.
 */
public class FlowConfiguringMessageProcessor
    implements FlowConstructAware, Lifecycle, InterceptingMessageProcessor, MessageProcessorContainer, NonBlockingSupported {

  private MessageProcessorBuilder builder;
  private MessageProcessor messageProcessor;
  private MessageProcessor listener;

  public FlowConfiguringMessageProcessor(MessageProcessorBuilder builder) {
    this.builder = builder;
  }

  public void setListener(MessageProcessor listener) {
    this.listener = listener;
  }

  public MuleEvent process(MuleEvent event) throws MuleException {
    return messageProcessor.process(event);
  }

  public void start() throws MuleException {
    if (messageProcessor instanceof Startable) {
      ((Startable) messageProcessor).start();
    }
  }

  public void setFlowConstruct(FlowConstruct flowConstruct) {
    if (builder instanceof FlowConstructAware) {
      ((FlowConstructAware) builder).setFlowConstruct(flowConstruct);
    }
  }

  public void dispose() {
    if (messageProcessor instanceof Disposable) {
      ((Disposable) messageProcessor).dispose();
    }
  }

  public void stop() throws MuleException {
    if (messageProcessor instanceof Stoppable) {
      ((Stoppable) messageProcessor).stop();
    }
  }

  public void initialise() throws InitialisationException {
    if (builder instanceof Initialisable) {
      ((Initialisable) builder).initialise();
    }

    try {
      messageProcessor = builder.build();
    } catch (Exception e) {
      throw new InitialisationException(e, this);
    }

    if (messageProcessor instanceof Initialisable) {
      ((Initialisable) messageProcessor).initialise();
    }

    if (messageProcessor instanceof InterceptingMessageProcessor && listener != null) {
      ((InterceptingMessageProcessor) messageProcessor).setListener(listener);
    }
  }

  /**
   * The MessageProcessor that this class built.
   */
  public MessageProcessor getWrappedMessageProcessor() {
    return messageProcessor;
  }

  public MessageProcessorBuilder getMessageProcessorBuilder() {
    return builder;
  }

  @Override
  public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement) {
    if (getWrappedMessageProcessor() instanceof MessageProcessorContainer) {
      ((MessageProcessorContainer) getWrappedMessageProcessor()).addMessageProcessorPathElements(pathElement);
    }
  }
}

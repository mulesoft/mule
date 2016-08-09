/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.endpoint;

import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAware;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.MessageProcessorContainer;
import org.mule.runtime.core.api.processor.MessageProcessorPathElement;
import org.mule.runtime.core.api.security.SecurityFilter;
import org.mule.runtime.core.processor.NonBlockingMessageProcessor;
import org.mule.runtime.core.processor.SecurityFilterMessageProcessor;
import org.mule.runtime.core.processor.chain.AbstractMessageProcessorChain;
import org.mule.runtime.core.processor.chain.SimpleMessageProcessorChainBuilder;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

public class EndpointMessageProcessorChainBuilder extends SimpleMessageProcessorChainBuilder {

  protected ImmutableEndpoint endpoint;

  public EndpointMessageProcessorChainBuilder(ImmutableEndpoint endpoint, FlowConstruct flowConstruct) {
    super(flowConstruct);
    this.endpoint = endpoint;
  }

  public EndpointMessageProcessorChainBuilder(ImmutableEndpoint endpoint) {
    this.endpoint = endpoint;
  }

  @Override
  protected MessageProcessor initializeMessageProcessor(Object processor) throws MuleException {
    if (processor instanceof AbstractMessageProcessorChain) {
      processor = new EndpointAwareMessageProcessorChain((AbstractMessageProcessorChain) processor);
    } else if (processor instanceof SecurityFilterMessageProcessor) {
      processor = new EndpointAwareSecurityFilterMessageProcessor((SecurityFilterMessageProcessor) processor);
    }

    if (processor instanceof EndpointAware) {
      ((EndpointAware) processor).setEndpoint(endpoint);
    }

    return super.initializeMessageProcessor(processor);
  }

  private class EndpointAwareMessageProcessorChain implements NonBlockingMessageProcessor, MessageProcessorChain, Lifecycle,
      FlowConstructAware, MuleContextAware, EndpointAware, MessageProcessorContainer, MessagingExceptionHandlerAware {

    private AbstractMessageProcessorChain chain;

    private ImmutableEndpoint endpoint;

    public EndpointAwareMessageProcessorChain(AbstractMessageProcessorChain chain) {
      this.chain = chain;
    }

    @Override
    public void setEndpoint(ImmutableEndpoint ep) {
      this.endpoint = ep;
      for (MessageProcessor processor : chain.getMessageProcessors()) {
        if (processor instanceof EndpointAware) {
          ((EndpointAware) processor).setEndpoint(ep);
        }
      }
    }

    @Override
    public ImmutableEndpoint getEndpoint() {
      return endpoint;
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException {
      return chain.process(event);
    }

    @Override
    public String getName() {
      return chain.getName();
    }

    @Override
    public void initialise() throws InitialisationException {
      chain.initialise();
    }

    @Override
    public void start() throws MuleException {
      chain.start();
    }

    @Override
    public void stop() throws MuleException {
      chain.stop();
    }

    @Override
    public void dispose() {
      chain.dispose();
    }

    @Override
    public void setMessagingExceptionHandler(MessagingExceptionHandler messagingExceptionHandler) {
      chain.setMessagingExceptionHandler(messagingExceptionHandler);
    }

    @Override
    public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement) {
      chain.addMessageProcessorPathElements(pathElement);
    }

    @Override
    public void setMuleContext(MuleContext context) {
      chain.setMuleContext(context);
    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct) {
      chain.setFlowConstruct(flowConstruct);
    }

    @Override
    public List<MessageProcessor> getMessageProcessors() {
      return chain.getMessageProcessors();
    }
  }

  private class EndpointAwareSecurityFilterMessageProcessor extends SecurityFilterMessageProcessor implements EndpointAware {

    private SecurityFilterMessageProcessor sfmp;

    private ImmutableEndpoint endpoint;

    public EndpointAwareSecurityFilterMessageProcessor(SecurityFilterMessageProcessor sfmp) {
      this.sfmp = sfmp;
    }

    @Override
    public void setEndpoint(ImmutableEndpoint ep) {
      this.endpoint = ep;
      if (sfmp.getFilter() instanceof EndpointAware) {
        ((EndpointAware) sfmp.getFilter()).setEndpoint(ep);
      }
    }

    @Override
    public ImmutableEndpoint getEndpoint() {
      return endpoint;
    }

    @Override
    public void setAnnotations(Map<QName, Object> newAnnotations) {
      sfmp.setAnnotations(newAnnotations);
    }

    @Override
    public int hashCode() {
      return sfmp.hashCode();
    }

    @Override
    public void initialise() throws InitialisationException {
      sfmp.initialise();
    }

    @Override
    public SecurityFilter getFilter() {
      return sfmp.getFilter();
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException {
      return sfmp.process(event);
    }

    @Override
    public void setFilter(SecurityFilter filter) {
      sfmp.setFilter(filter);
    }

    @Override
    public void setMuleContext(MuleContext context) {
      sfmp.setMuleContext(context);
    }

    @Override
    public void setListener(MessageProcessor next) {
      sfmp.setListener(next);
    }

    @Override
    public boolean equals(Object obj) {
      return sfmp.equals(obj);
    }

    @Override
    public MuleContext getMuleContext() {
      return sfmp.getMuleContext();
    }

    @Override
    public String toString() {
      return sfmp.toString();
    }

    @Override
    public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement) {
      sfmp.addMessageProcessorPathElements(pathElement);
    }
  }
}

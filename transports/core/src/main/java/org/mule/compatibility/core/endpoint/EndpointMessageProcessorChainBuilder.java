/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.endpoint;

import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAware;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.MessageProcessorPathElement;
import org.mule.runtime.core.api.security.SecurityFilter;
import org.mule.runtime.core.processor.NonBlockingMessageProcessor;
import org.mule.runtime.core.processor.SecurityFilterMessageProcessor;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChainBuilder;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

public class EndpointMessageProcessorChainBuilder extends DefaultMessageProcessorChainBuilder {

  protected ImmutableEndpoint endpoint;

  public EndpointMessageProcessorChainBuilder(ImmutableEndpoint endpoint) {
    this.endpoint = endpoint;
  }

  @Override
  protected Processor initializeMessageProcessor(Object processor) {
    if (processor instanceof MessageProcessorChain) {
      processor = new EndpointAwareMessageProcessorChain((MessageProcessorChain) processor);
    } else if (processor instanceof SecurityFilterMessageProcessor) {
      processor = new EndpointAwareSecurityFilterMessageProcessor((SecurityFilterMessageProcessor) processor);
    }

    if (processor instanceof EndpointAware) {
      ((EndpointAware) processor).setEndpoint(endpoint);
    }

    return super.initializeMessageProcessor(processor);
  }

  private class EndpointAwareMessageProcessorChain
      implements NonBlockingMessageProcessor, MessageProcessorChain, EndpointAware, MessagingExceptionHandlerAware {

    private MessageProcessorChain chain;

    private ImmutableEndpoint endpoint;

    public EndpointAwareMessageProcessorChain(MessageProcessorChain chain) {
      this.chain = chain;
    }

    @Override
    public void setEndpoint(ImmutableEndpoint ep) {
      this.endpoint = ep;
      for (Processor processor : chain.getMessageProcessors()) {
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
    public Event process(Event event) throws MuleException {
      return chain.process(event);
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
    public List<Processor> getMessageProcessors() {
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
    public Event process(Event event) throws MuleException {
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
    public void setListener(Processor next) {
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

  }
}

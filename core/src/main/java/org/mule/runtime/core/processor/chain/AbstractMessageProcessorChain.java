/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.chain;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import org.mule.runtime.core.AbstractAnnotatedObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAware;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.MessageProcessorContainer;
import org.mule.runtime.core.api.processor.MessageProcessorPathElement;
import org.mule.runtime.core.processor.NonBlockingMessageProcessor;
import org.mule.runtime.core.util.NotificationUtils;
import org.mule.runtime.core.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builder needs to return a composite rather than the first MessageProcessor in the chain. This is so that if this chain is
 * nested in another chain the next MessageProcessor in the parent chain is not injected into the first in the nested chain.
 */
public abstract class AbstractMessageProcessorChain extends AbstractAnnotatedObject
    implements NonBlockingMessageProcessor, MessageProcessorChain, Lifecycle, FlowConstructAware, MuleContextAware,
    MessageProcessorContainer, MessagingExceptionHandlerAware {

  protected final transient Logger log = LoggerFactory.getLogger(getClass());
  protected String name;
  protected List<MessageProcessor> processors;
  protected FlowConstruct flowConstruct;

  public AbstractMessageProcessorChain(String name, List<MessageProcessor> processors) {
    this.name = name;
    this.processors = processors;
  }

  @Override
  public MuleEvent process(MuleEvent event) throws MuleException {
    if (log.isDebugEnabled()) {
      log.debug(String.format("Invoking %s with event %s", this, event));
    }
    if (event == null) {
      return null;
    }

    return doProcess(event);
  }

  protected abstract MuleEvent doProcess(MuleEvent event) throws MuleException;

  @Override
  public void initialise() throws InitialisationException {
    for (MessageProcessor processor : processors) {
      // MULE-5002 TODO review MP Lifecycle
      initialiseIfNeeded(processor);
    }
  }

  @Override
  public void start() throws MuleException {
    List<MessageProcessor> startedProcessors = new ArrayList<MessageProcessor>();
    try {
      for (MessageProcessor processor : processors) {
        if (processor instanceof Startable) {
          ((Startable) processor).start();
          startedProcessors.add(processor);
        }
      }
    } catch (MuleException e) {
      stop(startedProcessors);
      throw e;
    }
  }

  private void stop(List<MessageProcessor> processorsToStop) throws MuleException {
    for (MessageProcessor processor : processorsToStop) {
      if (processor instanceof Stoppable) {
        ((Stoppable) processor).stop();
      }
    }
  }

  @Override
  public void stop() throws MuleException {
    stop(processors);
  }

  @Override
  public void dispose() {
    for (MessageProcessor processor : processors) {
      if (processor instanceof Disposable) {
        ((Disposable) processor).dispose();
      }
    }
    processors.clear();
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    for (MessageProcessor processor : processors) {
      if (processor instanceof FlowConstructAware) {
        ((FlowConstructAware) processor).setFlowConstruct(flowConstruct);
      }
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    StringBuilder string = new StringBuilder();
    string.append(getClass().getSimpleName());
    if (StringUtils.isNotBlank(name)) {
      string.append(String.format(" '%s' ", name));
    }

    Iterator<MessageProcessor> mpIterator = processors.iterator();

    final String nl = String.format("%n");

    // TODO have it print the nested structure with indents increasing for nested MPCs
    if (mpIterator.hasNext()) {
      string.append(String.format("%n[ "));
      while (mpIterator.hasNext()) {
        MessageProcessor mp = mpIterator.next();
        final String indented = StringUtils.replace(mp.toString(), nl, String.format("%n  "));
        string.append(String.format("%n  %s", indented));
        if (mpIterator.hasNext()) {
          string.append(", ");
        }
      }
      string.append(String.format("%n]"));
    }

    return string.toString();
  }

  @Override
  public List<MessageProcessor> getMessageProcessors() {
    return processors;
  }

  @Override
  public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement) {
    NotificationUtils.addMessageProcessorPathElements(getMessageProcessors(), pathElement);

  }

  @Override
  public void setMessagingExceptionHandler(MessagingExceptionHandler messagingExceptionHandler) {
    for (MessageProcessor processor : processors) {
      if (processor instanceof MessagingExceptionHandlerAware) {
        ((MessagingExceptionHandlerAware) processor).setMessagingExceptionHandler(messagingExceptionHandler);
      }
    }
  }

  @Override
  public void setMuleContext(MuleContext context) {

    for (MessageProcessor processor : processors) {
      if (processor instanceof MuleContextAware) {
        ((MuleContextAware) processor).setMuleContext(context);
      }
    }
  }

}

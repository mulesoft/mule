/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.execution.LocationExecutionContextProvider;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.processor.InterceptingMessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.routing.filter.FilterUnacceptedException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.processor.AbstractFilteringMessageProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link InterceptingMessageProcessor} that filters message flow using a {@link Filter}. Is the filter accepts
 * the message then message flow continues to the next message processor. If the filter does not accept the message processor and
 * a message processor is configured for handling unaccepted message then this will be invoked, otherwise <code>null</code> will
 * be returned.
 * <p/>
 * <b>EIP Reference:</b> <a href="http://www.eaipatterns.com/Filter.html">http://www.eaipatterns .com/Filter.html<a/>
 */
public class MessageFilter extends AbstractFilteringMessageProcessor implements FlowConstructAware, Lifecycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(MessageFilter.class);

  protected Filter filter;

  /**
   * For IoC only
   *
   * @deprecated Use MessageFilter(Filter filter)
   */
  @Deprecated
  public MessageFilter() {
    super();
  }

  public MessageFilter(Filter filter) {
    super();
    this.filter = filter;
  }

  /**
   * @param filter
   * @param throwExceptionOnUnaccepted throw a FilterUnacceptedException when a message is rejected by the filter?
   * @param messageProcessor used to handler unaccepted messages
   */
  public MessageFilter(Filter filter, boolean throwExceptionOnUnaccepted, MessageProcessor messageProcessor) {
    this.filter = filter;
    this.throwOnUnaccepted = throwExceptionOnUnaccepted;
    this.unacceptedMessageProcessor = messageProcessor;
    setUnacceptedMessageProcessor(unacceptedMessageProcessor);
  }

  @Override
  protected boolean accept(MuleEvent event) {
    if (filter == null) {
      return true;
    }

    if (event != null && !VoidMuleEvent.getInstance().equals(event)) {
      return filter.accept(event);
    } else {
      return false;
    }
  }

  @Override
  protected MessagingException filterFailureException(MuleEvent event, Exception ex) {
    MessagingException messagingException = new MessagingException(event, ex, this);
    String docName = LocationExecutionContextProvider.getDocName(filter);
    messagingException.getInfo().put("Filter",
                                     docName != null ? String.format("%s (%s)", filter.toString(), docName) : filter.toString());
    return messagingException;
  }

  @Override
  protected MuleException filterUnacceptedException(MuleEvent event) {
    return new FilterUnacceptedException(CoreMessages.messageRejectedByFilter(), event, filter, this);
  }

  public Filter getFilter() {
    return filter;
  }

  public void setFilter(Filter filter) {
    this.filter = filter;
  }

  @Override
  public String toString() {
    return (filter == null ? "null filter" : filter.getClass().getName()) + " (wrapped by " + this.getClass().getSimpleName()
        + ")";
  }

  @Override
  public void setMuleContext(MuleContext context) {
    super.setMuleContext(context);
    if (!onUnacceptedFlowConstruct && unacceptedMessageProcessor instanceof MuleContextAware) {
      ((MuleContextAware) unacceptedMessageProcessor).setMuleContext(context);
    }
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    if (!onUnacceptedFlowConstruct && unacceptedMessageProcessor instanceof FlowConstructAware) {
      ((FlowConstructAware) unacceptedMessageProcessor).setFlowConstruct(flowConstruct);
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    if (!onUnacceptedFlowConstruct && unacceptedMessageProcessor instanceof Initialisable) {
      ((Initialisable) unacceptedMessageProcessor).initialise();
    }

    LifecycleUtils.initialiseIfNeeded(filter);
  }

  @Override
  public void start() throws MuleException {
    if (!onUnacceptedFlowConstruct && unacceptedMessageProcessor instanceof Startable) {
      ((Startable) unacceptedMessageProcessor).start();
    }
  }

  @Override
  public void stop() throws MuleException {
    if (!onUnacceptedFlowConstruct && unacceptedMessageProcessor instanceof Stoppable) {
      ((Stoppable) unacceptedMessageProcessor).stop();
    }
  }

  @Override
  public void dispose() {
    if (!onUnacceptedFlowConstruct && unacceptedMessageProcessor instanceof Disposable) {
      ((Disposable) unacceptedMessageProcessor).dispose();
    }

    LifecycleUtils.disposeIfNeeded(filter, LOGGER);
  }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.exception.MessagingException;

/**
 * Template for executing a MessageProcessor.
 *
 * Provides all a set of functionality common to all MessageProcessor execution.
 */
public class MessageProcessorExecutionTemplate implements MuleContextAware, FlowConstructAware {

  private MessageProcessorExecutionInterceptor executionInterceptor;

  private MessageProcessorExecutionTemplate(MessageProcessorExecutionInterceptor executionInterceptor) {
    this.executionInterceptor = executionInterceptor;
  }

  public static MessageProcessorExecutionTemplate createExecutionTemplate() {
    return new MessageProcessorExecutionTemplate(new MessageProcessorNotificationExecutionInterceptor(new ExceptionToMessagingExceptionExecutionInterceptor()));
  }

  public static MessageProcessorExecutionTemplate createNotificationExecutionTemplate() {
    return new MessageProcessorExecutionTemplate(new MessageProcessorNotificationExecutionInterceptor());
  }

  public Event execute(Processor messageProcessor, Event event) throws MessagingException {
    return this.executionInterceptor.execute(messageProcessor, event);
  }

  @Override
  public void setMuleContext(MuleContext context) {
    executionInterceptor.setMuleContext(context);
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    executionInterceptor.setFlowConstruct(flowConstruct);
  }
}

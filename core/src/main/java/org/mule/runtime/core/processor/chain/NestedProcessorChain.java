/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.chain;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.NestedProcessor;
import org.mule.runtime.core.api.processor.MessageProcessor;

import java.util.Map;

/**
 * An implementation of {@link NestedProcessor} that wraps a {@link MessageProcessor} and allows to execute it
 *
 * @since 3.7.0
 */
public class NestedProcessorChain implements NestedProcessor {

  /**
   * Chain that will be executed upon calling process
   */
  private MessageProcessor chain;
  /**
   * Event that will be cloned for dispatching
   */
  private MuleEvent event;

  public NestedProcessorChain(MuleEvent event, MessageProcessor chain) {
    this.event = event;
    this.chain = chain;
  }

  /**
   * Sets chain
   *
   * @param value Value to set
   */
  public void setChain(MessageProcessor value) {
    this.chain = value;
  }

  /**
   * Sets event
   *
   * @param value Value to set
   */
  public void setEvent(MuleEvent value) {
    this.event = value;
  }

  @Override
  public Object process() throws Exception {
    MuleEvent muleEvent;
    muleEvent = new DefaultMuleEvent(event.getMessage(), event);
    return chain.process(muleEvent).getMessage().getPayload();
  }

  @Override
  public Object process(Object payload) throws Exception {
    MuleEvent muleEvent = new DefaultMuleEvent(MuleMessage.builder().payload(payload).build(), event);
    return chain.process(muleEvent).getMessage().getPayload();
  }

  @Override
  public Object processWithExtraProperties(Map<String, Object> properties) throws Exception {
    MuleMessage muleMessage;
    muleMessage = event.getMessage();
    for (String property : properties.keySet()) {
      event.setFlowVariable(property, properties.get(property));
    }
    MuleEvent muleEvent;
    muleEvent = new DefaultMuleEvent(muleMessage, event);
    return chain.process(muleEvent).getMessage().getPayload();
  }

  @Override
  public Object process(Object payload, Map<String, Object> properties) throws Exception {
    MuleEvent muleEvent = new DefaultMuleEvent(MuleMessage.builder().payload(payload).build(), event);
    for (String property : properties.keySet()) {
      muleEvent.setFlowVariable(property, properties.get(property));
    }
    return chain.process(muleEvent).getMessage().getPayload();
  }
}

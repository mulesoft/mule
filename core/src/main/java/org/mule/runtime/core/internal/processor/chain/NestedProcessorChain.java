/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.chain;

import static org.mule.runtime.api.message.Message.of;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.NestedProcessor;
import org.mule.runtime.core.api.processor.Processor;

import java.util.Map;

/**
 * An implementation of {@link NestedProcessor} that wraps a {@link Processor} and allows to execute it
 *
 * @since 3.7.0
 */
public class NestedProcessorChain implements NestedProcessor {

  /**
   * Chain that will be executed upon calling process
   */
  private Processor chain;
  /**
   * Event that will be cloned for dispatching
   */
  private Event event;

  public NestedProcessorChain(Event event, Processor chain) {
    this.event = event;
    this.chain = chain;
  }

  /**
   * Sets chain
   *
   * @param value Value to set
   */
  public void setChain(Processor value) {
    this.chain = value;
  }

  /**
   * Sets event
   *
   * @param value Value to set
   */
  public void setEvent(Event value) {
    this.event = value;
  }

  @Override
  public Object process() throws Exception {
    return chain.process(event).getMessage().getPayload().getValue();
  }

  @Override
  public Object process(Object payload) throws Exception {
    Event muleEvent = Event.builder(event).message(of(payload)).build();
    return chain.process(muleEvent).getMessage().getPayload().getValue();
  }

  @Override
  public Object processWithExtraProperties(Map<String, Object> properties) throws Exception {
    Event muleEvent = Event.builder(event).variables(properties).build();
    return chain.process(muleEvent).getMessage().getPayload().getValue();
  }

  @Override
  public Object process(Object payload, Map<String, Object> properties) throws Exception {
    Event muleEvent = Event.builder(event).message(of(payload)).variables(properties).build();
    return chain.process(muleEvent).getMessage().getPayload().getValue();
  }

}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration;

import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import static org.mule.tck.junit4.AbstractMuleTestCase.TEST_CONNECTOR;

import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.ClusterizableMessageSource;

public class TestClusterizableMessageSource
    implements ClusterizableMessageSource, Startable, FlowConstructAware {

  private Processor listener;
  private FlowConstruct flowConstruct;

  @Override
  public void start() throws MuleException {
    InternalMessage muleMessage = InternalMessage.builder().payload("TEST").build();
    Event defaultMuleEvent = Event.builder(DefaultEventContext.create(flowConstruct, TEST_CONNECTOR))
        .message(muleMessage).exchangePattern(ONE_WAY).flow(flowConstruct).build();
    listener.process(defaultMuleEvent);
  }

  @Override
  public void setListener(Processor listener) {
    this.listener = listener;
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }
}

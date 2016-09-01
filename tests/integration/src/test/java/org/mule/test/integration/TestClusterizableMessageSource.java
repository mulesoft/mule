/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration;

import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import static org.mule.tck.junit4.AbstractMuleTestCase.TEST_CONNECTOR;

import org.mule.runtime.core.DefaultMessageContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.source.ClusterizableMessageSource;

public class TestClusterizableMessageSource
    implements ClusterizableMessageSource, Startable, FlowConstructAware {

  private MessageProcessor listener;
  private FlowConstruct flowConstruct;

  @Override
  public void start() throws MuleException {
    MuleMessage muleMessage = MuleMessage.builder().payload("TEST").build();
    MuleEvent defaultMuleEvent = MuleEvent.builder(DefaultMessageContext.create(flowConstruct, TEST_CONNECTOR))
        .message(muleMessage).exchangePattern(ONE_WAY).flow(flowConstruct).build();
    listener.process(defaultMuleEvent);
  }

  @Override
  public void setListener(MessageProcessor listener) {
    this.listener = listener;
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }
}

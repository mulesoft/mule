/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.source.ClusterizableMessageSource;

public class TestClusterizableMessageSource
    implements ClusterizableMessageSource, Startable, MuleContextAware, FlowConstructAware {

  private MessageProcessor listener;
  private MuleContext context;
  private FlowConstruct flowConstruct;

  @Override
  public void start() throws MuleException {
    MuleMessage muleMessage = MuleMessage.builder().payload("TEST").build();
    DefaultMuleEvent defaultMuleEvent = new DefaultMuleEvent(muleMessage, MessageExchangePattern.ONE_WAY, flowConstruct);
    listener.process(defaultMuleEvent);
  }

  @Override
  public void setListener(MessageProcessor listener) {
    this.listener = listener;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.context = context;
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }
}

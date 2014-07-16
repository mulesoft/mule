/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Startable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.ClusterizableMessageSource;

public class TestClusterizableMessageSource implements ClusterizableMessageSource, Startable, MuleContextAware, FlowConstructAware
{

    private MessageProcessor listener;
    private MuleContext context;
    private FlowConstruct flowConstruct;

    @Override
    public void start() throws MuleException
    {
        DefaultMuleMessage muleMessage = new DefaultMuleMessage("TEST", context);
        DefaultMuleEvent defaultMuleEvent = new DefaultMuleEvent(muleMessage, MessageExchangePattern.ONE_WAY, flowConstruct);
        listener.process(defaultMuleEvent);
    }

    @Override
    public void setListener(MessageProcessor listener)
    {
        this.listener = listener;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.context = context;
    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }
}

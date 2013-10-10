/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

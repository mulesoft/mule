/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor.chain;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.NestedProcessor;
import org.mule.api.context.MuleContextAware;
import org.mule.api.processor.MessageProcessor;

import java.util.Map;

/**
 * An implementation of {@link NestedProcessor} that
 * wraps a {@link MessageProcessor} and allows to
 * execute it
 *
 * @since 3.7.0
 */
public class NestedProcessorChain implements NestedProcessor, MuleContextAware
{

    /**
     * Mule Context
     */
    private MuleContext muleContext;
    /**
     * Chain that will be executed upon calling process
     */
    private MessageProcessor chain;
    /**
     * Event that will be cloned for dispatching
     */
    private MuleEvent event;

    public NestedProcessorChain(MuleEvent event, MuleContext muleContext, MessageProcessor chain)
    {
        this.event = event;
        this.chain = chain;
        this.muleContext = muleContext;
    }

    /**
     * Sets muleContext
     *
     * @param value Value to set
     */
    public void setMuleContext(MuleContext value)
    {
        this.muleContext = value;
    }

    /**
     * Sets chain
     *
     * @param value Value to set
     */
    public void setChain(MessageProcessor value)
    {
        this.chain = value;
    }

    /**
     * Sets event
     *
     * @param value Value to set
     */
    public void setEvent(MuleEvent value)
    {
        this.event = value;
    }

    public Object process() throws Exception
    {
        MuleEvent muleEvent;
        muleEvent = new DefaultMuleEvent(event.getMessage(), event);
        return chain.process(muleEvent).getMessage().getPayload();
    }

    public Object process(Object payload) throws Exception
    {
        MuleMessage muleMessage;
        muleMessage = new DefaultMuleMessage(payload, muleContext);
        MuleEvent muleEvent;
        muleEvent = new DefaultMuleEvent(muleMessage, event);
        return chain.process(muleEvent).getMessage().getPayload();
    }

    public Object processWithExtraProperties(Map<String, Object> properties) throws Exception
    {
        MuleMessage muleMessage;
        muleMessage = event.getMessage();
        for (String property : properties.keySet())
        {
            muleMessage.setInvocationProperty(property, properties.get(property));
        }
        MuleEvent muleEvent;
        muleEvent = new DefaultMuleEvent(muleMessage, event);
        return chain.process(muleEvent).getMessage().getPayload();
    }

    public Object process(Object payload, Map<String, Object> properties) throws Exception
    {
        MuleMessage muleMessage;
        muleMessage = new DefaultMuleMessage(payload, muleContext);
        for (String property : properties.keySet())
        {
            muleMessage.setInvocationProperty(property, properties.get(property));
        }
        MuleEvent muleEvent;
        muleEvent = new DefaultMuleEvent(muleMessage, event);
        return chain.process(muleEvent).getMessage().getPayload();
    }

}
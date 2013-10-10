/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.config;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorBuilder;

/**
 * Wraps a {@link MessageProcessorBuilder} and configures it lazily so it can
 * be injected with the {@link FlowConstruct}.
 */
public class FlowConfiguringMessageProcessor implements FlowConstructAware, Lifecycle, InterceptingMessageProcessor
{

    private MessageProcessorBuilder builder;
    private MessageProcessor messageProcessor;
    private MessageProcessor listener;

    public FlowConfiguringMessageProcessor(MessageProcessorBuilder builder)
    {
        this.builder = builder;
    }

    public void setListener(MessageProcessor listener)
    {
        this.listener = listener;
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        return messageProcessor.process(event);
    }

    public void start() throws MuleException
    {
        if (messageProcessor instanceof Startable)
        {
            ((Startable) messageProcessor).start();
        }
    }

    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        if (builder instanceof FlowConstructAware)
        {
            ((FlowConstructAware) builder).setFlowConstruct(flowConstruct);
        }
    }

    public void dispose()
    {
        if (messageProcessor instanceof Disposable)
        {
            ((Disposable) messageProcessor).dispose();
        }
    }

    public void stop() throws MuleException
    {
        if (messageProcessor instanceof Stoppable)
        {
            ((Stoppable) messageProcessor).stop();
        }
    }

    public void initialise() throws InitialisationException
    {
        if (builder instanceof Initialisable)
        {
            ((Initialisable) builder).initialise();
        }

        try
        {
            messageProcessor = builder.build();
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }

        if (messageProcessor instanceof Initialisable)
        {
            ((Initialisable) messageProcessor).initialise();
        }

        if (messageProcessor instanceof InterceptingMessageProcessor && listener != null)
        {
            ((InterceptingMessageProcessor) messageProcessor).setListener(listener);
        }
    }

    /**
     * The MessageProcessor that this class built.
     */
    public MessageProcessor getWrappedMessageProcessor()
    {
        return messageProcessor;
    }

    public MessageProcessorBuilder getMessageProcessorBuilder()
    {
        return builder;
    }

}

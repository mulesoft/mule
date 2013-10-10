/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.exception;

import org.mule.VoidMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.processor.AbstractMessageProcessorOwner;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RedeliveryExceeded implements MessageProcessor, FlowConstructAware, Initialisable
{
    private List<MessageProcessor> messageProcessors = new CopyOnWriteArrayList();
    private MessageProcessorChain configuredMessageProcessors;
    private FlowConstruct flowConstruct;

    @Override
    public void initialise() throws InitialisationException
    {
        DefaultMessageProcessorChainBuilder defaultMessageProcessorChainBuilder = new DefaultMessageProcessorChainBuilder(this.flowConstruct);
        try
        {
            configuredMessageProcessors = defaultMessageProcessorChainBuilder.chain(messageProcessors).build();
        }
        catch (MuleException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    public List<MessageProcessor> getMessageProcessors()
    {
        return Collections.unmodifiableList(messageProcessors);
    }

    public void setMessageProcessors(List<MessageProcessor> processors)
    {
        if (processors != null)
        {
            this.messageProcessors.clear();
            this.messageProcessors.addAll(processors);
        }
        else
        {
            throw new IllegalArgumentException("List of targets = null");
        }
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        MuleEvent result = event;
        if (!messageProcessors.isEmpty()) {
            result = configuredMessageProcessors.process(event);
        }
        if (result != null && !VoidMuleEvent.getInstance().equals(result))
        {
            result.getMessage().setExceptionPayload(null);
        }
        return result;
    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }
}

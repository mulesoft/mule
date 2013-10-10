/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.processor;

import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.CoreMessages;
import org.mule.routing.MessageProcessorFilterPair;

/**
 * Implement a redelivery policy for Mule.  This is similar to JMS retry policies that will redeliver a message a maximum
 * number of times.  If this maximum is exceeded, the message is sent to a dead letter queue,  Here, if the processing of the messages
 * fails too often, the message is sent to the failedMessageProcessor MP, whence success is force to be returned, to allow
 * the message to be considered "consumed".
 */
public abstract class AbstractRedeliveryPolicy extends AbstractInterceptingMessageProcessor implements MessageProcessor, Lifecycle, MuleContextAware, FlowConstructAware
{

    protected FlowConstruct flowConstruct;
    protected int maxRedeliveryCount;
    protected MessageProcessor deadLetterQueue;
    public static final int REDELIVERY_FAIL_ON_FIRST = 0;

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
        if (deadLetterQueue instanceof FlowConstructAware)
        {
            ((FlowConstructAware) deadLetterQueue).setFlowConstruct(flowConstruct);
        }
    }

    @Override
    public void initialise() throws InitialisationException
    {
        if (maxRedeliveryCount < 0)
        {
            throw new InitialisationException(
                CoreMessages.initialisationFailure(
                    "maxRedeliveryCount must be positive"), this);
        }

        if (deadLetterQueue instanceof Initialisable)
        {
            ((Initialisable) deadLetterQueue).initialise();
        }
    }

    @Override
    public void start() throws MuleException
    {
        if (deadLetterQueue instanceof Startable)
        {
            ((Startable) deadLetterQueue).start();
        }
    }

    @Override
    public void stop() throws MuleException
    {
        if (deadLetterQueue instanceof Stoppable)
        {
            ((Stoppable) deadLetterQueue).stop();
        }
    }

    @Override
    public void dispose()
    {
        if (deadLetterQueue instanceof Disposable)
        {
            ((Disposable) deadLetterQueue).dispose();
        }
    }

    public int getMaxRedeliveryCount()
    {
        return maxRedeliveryCount;
    }

    public void setMaxRedeliveryCount(int maxRedeliveryCount)
    {
        this.maxRedeliveryCount = maxRedeliveryCount;
    }

    public MessageProcessor getTheFailedMessageProcessor()
    {
        return deadLetterQueue;
    }

    public void setDeadLetterQueue(MessageProcessorFilterPair failedMessageProcessorPair)
    {
        this.deadLetterQueue = failedMessageProcessorPair.getMessageProcessor();
    }
}

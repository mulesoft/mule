/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor.chain;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.context.notification.MessageProcessorNotification;

import java.util.List;

/**
 * Builder needs to return a composite rather than the first MessageProcessor in the
 * chain. This is so that if this chain is nested in another chain the next
 * MessageProcessor in the parent chain is not injected into the first in the nested
 * chain.
 */
public class InterceptingChainLifecycleWrapper extends AbstractMessageProcessorChain
{
    private MessageProcessorChain chain;

    public InterceptingChainLifecycleWrapper(MessageProcessorChain chain,
                                             List<MessageProcessor> processors,
                                             String name)
    {
        super(name, processors);
        this.chain = chain;
    }

    @Override
    public List<MessageProcessor> getMessageProcessors()
    {
        return chain.getMessageProcessors();
    }

    @Override
    public String getName()
    {
        return chain.getName();
    }

    @Override
    protected MuleEvent doProcess(MuleEvent event) throws MuleException
    {
        return chain.process(event);
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        super.setMuleContext(context);
        for (MessageProcessor processor : processors)
        {
            if (processor instanceof MuleContextAware)
            {
                ((MuleContextAware) processor).setMuleContext(context);
            }
        }
        if (chain instanceof MuleContextAware)
        {
            ((MuleContextAware) chain).setMuleContext(context);
        }
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (event == null)
        {
            return null;
        }

        boolean fireNotification = event.isNotificationsEnabled();

        if (fireNotification)
        {
            fireNotification(event.getFlowConstruct(), event, this,
                             MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE);
        }

        MuleEvent result = super.process(event);

        if (fireNotification)
        {
            fireNotification(event.getFlowConstruct(), result, this,
                             MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE);
        }

        return result;
    }
}

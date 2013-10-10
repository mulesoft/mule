/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.MuleContextAware;
import org.mule.api.context.notification.ServerNotificationHandler;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.context.notification.MessageProcessorNotification;
import org.mule.processor.chain.DefaultMessageProcessorChain;
import org.mule.util.ObjectUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract implementation of {@link InterceptingMessageProcessor} that simply
 * provides an implementation of setNext and holds the next message processor as an
 * attribute.
 */
public abstract class AbstractInterceptingMessageProcessor
    implements InterceptingMessageProcessor, MuleContextAware
{
    protected Log logger = LogFactory.getLog(getClass());

    protected ServerNotificationHandler notificationHandler;

    protected MuleContext muleContext;

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
        notificationHandler = muleContext.getNotificationManager();
        if (next instanceof DefaultMessageProcessorChain)
        {
            ((DefaultMessageProcessorChain) next).setMuleContext(context);
        }
    }

    public void setListener(MessageProcessor next)
    {
        this.next = next;
    }

    protected MessageProcessor next;

    protected MuleEvent processNext(MuleEvent event) throws MuleException
    {
        if (next == null)
        {
            return event;
        }
        else if (event == null)
        {
            if (logger.isDebugEnabled())
            {
                logger.trace("MuleEvent is null.  Next MessageProcessor '" + next.getClass().getName()
                             + "' will not be invoked.");
            }
            return null;
        }
        else
        {
            if (logger.isTraceEnabled())
            {
                logger.trace("Invoking next MessageProcessor: '" + next.getClass().getName() + "' ");
            }

            boolean fireNotification = !(next instanceof MessageProcessorChain);

            if (fireNotification)
            {
                // note that we're firing event for the next in chain, not this MP
                fireNotification(event.getFlowConstruct(), event, next,
                    MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE);
            }
            final MuleEvent result = next.process(event);
            if (fireNotification)
            {
                fireNotification(event.getFlowConstruct(), result, next,
                    MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE);
            }
            return result;
        }
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    @Override
    public String toString()
    {
        return ObjectUtils.toString(this);
    }

    protected void fireNotification(FlowConstruct flowConstruct, MuleEvent event, MessageProcessor processor, int action)
    {
        if (notificationHandler != null
            && notificationHandler.isNotificationEnabled(MessageProcessorNotification.class))
        {
            notificationHandler.fireNotification(new MessageProcessorNotification(flowConstruct, event, processor, action));
        }
    }

}

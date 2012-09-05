/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.execution;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.notification.ServerNotificationHandler;
import org.mule.api.processor.MessageProcessor;
import org.mule.construct.AbstractPipeline;
import org.mule.context.notification.MessageProcessorNotification;
import org.mule.context.notification.ServerNotificationManager;

/**
 * Intercepts MessageProcessor execution to fire before and after notifications
 */
class MessageProcessorNotificationExecutionInterceptor implements MessageProcessorExecutionInterceptor
{
    private MessageProcessorExecutionInterceptor next;

    MessageProcessorNotificationExecutionInterceptor(MessageProcessorExecutionInterceptor next)
    {
        this.next = next;
    }

    @Override
    public MuleEvent execute(MessageProcessor messageProcessor, MuleEvent event) throws MessagingException
    {
        ServerNotificationManager notificationManager = event.getMuleContext().getNotificationManager();
        fireNotification(notificationManager,event.getFlowConstruct(), event, messageProcessor,
                                null, MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE);

        MuleEvent result = null;
        MessagingException exceptionThrown = null;
        try
        {
            result = next.execute(messageProcessor,event);
        }
        catch (MessagingException e)
        {
            exceptionThrown = e;
            throw e;
        }
        finally
        {
            fireNotification(notificationManager, event.getFlowConstruct(), result != null ? result : event, messageProcessor,
                                exceptionThrown, MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE);
        }
        return result;
    }

    protected void fireNotification(ServerNotificationManager serverNotificationManager, FlowConstruct flowConstruct, MuleEvent event, MessageProcessor processor, MessagingException exceptionThrown, int action)
    {
        if (serverNotificationManager != null
            && serverNotificationManager.isNotificationEnabled(MessageProcessorNotification.class))
        {
            if (flowConstruct instanceof AbstractPipeline && ((AbstractPipeline) flowConstruct).getProcessorPath(processor) != null)
            {
                serverNotificationManager.fireNotification(new MessageProcessorNotification(flowConstruct, event, processor, exceptionThrown, action));
            }
        }
    }
}

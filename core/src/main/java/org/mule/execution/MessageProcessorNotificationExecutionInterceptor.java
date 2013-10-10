/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.execution;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.MessageProcessor;
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
        boolean fireNotification = event.isNotificationsEnabled();
        if (fireNotification)
        {
            fireNotification(notificationManager, event.getFlowConstruct(), event, messageProcessor,
                             null, MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE);
        }

        MuleEvent result = null;
        MessagingException exceptionThrown = null;
        try
        {
            result = next.execute(messageProcessor, event);
        }
        catch (MessagingException e)
        {
            exceptionThrown = e;
            throw e;
        }
        finally
        {
            if (fireNotification)
            {
                fireNotification(notificationManager, event.getFlowConstruct(), result != null ? result : event, messageProcessor,
                                 exceptionThrown, MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE);
            }
        }
        return result;
    }

    protected void fireNotification(ServerNotificationManager serverNotificationManager, FlowConstruct flowConstruct, MuleEvent event, MessageProcessor processor, MessagingException exceptionThrown, int action)
    {
        if (serverNotificationManager != null
            && serverNotificationManager.isNotificationEnabled(MessageProcessorNotification.class))
        {
            serverNotificationManager.fireNotification(new MessageProcessorNotification(flowConstruct, event, processor, exceptionThrown, action));
        }
    }
}

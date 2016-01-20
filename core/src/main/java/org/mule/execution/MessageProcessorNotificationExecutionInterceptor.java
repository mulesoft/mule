/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.execution;

import org.mule.OptimizedRequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.MessageProcessorPathResolver;
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

    MessageProcessorNotificationExecutionInterceptor()
    {

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

        // Update RequestContext ThreadLocal in case if previous processor modified it
        OptimizedRequestContext.unsafeSetEvent(event);

        MuleEvent result = null;
        MessagingException exceptionThrown = null;
        try
        {
            if (next == null)
            {
                result = messageProcessor.process(event);
            }
            else
            {
                result = next.execute(messageProcessor, event);
            }
        }
        catch (MessagingException e)
        {
            exceptionThrown = e;
            throw e;
        }
        catch (MuleException e)
        {
            exceptionThrown = new MessagingException(event, e, messageProcessor);
            throw exceptionThrown;
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
            if (flowConstruct instanceof MessageProcessorPathResolver && ((MessageProcessorPathResolver) flowConstruct).getProcessorPath(processor) != null)
            {
                serverNotificationManager.fireNotification(new MessageProcessorNotification(flowConstruct, event, processor, exceptionThrown, action));
            }
        }
    }
}

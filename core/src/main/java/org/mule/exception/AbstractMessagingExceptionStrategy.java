/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.exception;

import org.mule.DefaultMuleMessage;
import org.mule.RequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.RoutingException;
import org.mule.context.notification.ExceptionNotification;
import org.mule.message.DefaultExceptionPayload;
import org.mule.transport.NullPayload;
import org.mule.util.ObjectUtils;

/**
 * <code>DefaultExceptionStrategy</code> provides a default exception handling
 * strategy.
 */

public abstract class AbstractMessagingExceptionStrategy extends AbstractExceptionListener implements MessagingExceptionHandler
{
    /**
     * {@inheritDoc}
     */
    public MuleEvent handleException(Exception e, MuleEvent event)
    {
        if (enableNotifications)
        {
            fireNotification(new ExceptionNotification(e));
        }

        logException(e);
        handleTransaction(e);

        Throwable t = getExceptionType(e, RoutingException.class);
        if (t != null)
        {
            RoutingException re = (RoutingException) t;
            handleRoutingException(re.getMuleMessage(), re.getRoute(), e);

            event.getMessage().setPayload(NullPayload.getInstance());
            event.getMessage().setExceptionPayload(new DefaultExceptionPayload(e));
            return event;
        }

        t = getExceptionType(e, MessagingException.class);
        if (t != null)
        {
            MessagingException me = (MessagingException) t;
            handleMessagingException(me.getMuleMessage(), e);

            event.getMessage().setPayload(NullPayload.getInstance());
            event.getMessage().setExceptionPayload(new DefaultExceptionPayload(e));
            return event;
        }

        t = getExceptionType(e, LifecycleException.class);
        if (t != null)
        {
            LifecycleException le = (LifecycleException) t;
            handleLifecycleException(le.getComponent(), e);
            if (RequestContext.getEventContext() != null)
            {
                handleMessagingException(RequestContext.getEventContext().getMessage(), e);
            }
            else
            {
                logger.info("There is no current event available, routing Null message with the exception");
                handleMessagingException(new DefaultMuleMessage(NullPayload.getInstance(), muleContext), e);
            }
            event.getMessage().setPayload(NullPayload.getInstance());
            event.getMessage().setExceptionPayload(new DefaultExceptionPayload(e));
            return event;
        }

        handleStandardException(e);
        event.getMessage().setPayload(NullPayload.getInstance());
        event.getMessage().setExceptionPayload(new DefaultExceptionPayload(e));
        return event;
    }

    public void handleMessagingException(MuleMessage message, Throwable t)
    {
        defaultHandler(t);
        routeException(messageFromContextIfAvailable(message), null, t);
    }

    public void handleRoutingException(MuleMessage message, MessageProcessor target, Throwable t)
    {
        defaultHandler(t);
        routeException(messageFromContextIfAvailable(message), target, t);
    }

    public void handleLifecycleException(Object component, Throwable t)
    {
        // Do nothing special here. Overriding implmentations may want alter the
        // behaviour
        handleStandardException(t);
        logger.error("The object that failed was: \n" + ObjectUtils.toString(component, "null"));
    }

    public void handleStandardException(Throwable t)
    {
        // Attempt to send the exception details to an endpoint if one is set
        if (RequestContext.getEventContext() != null)
        {
            handleMessagingException(RequestContext.getEventContext().getMessage(), t);
        }
        else
        {
            logger.info("There is no current event available, routing Null message with the exception");
            handleMessagingException(new DefaultMuleMessage(NullPayload.getInstance(), muleContext), t);
        }
    }

    protected void defaultHandler(Throwable t)
    {
        if (RequestContext.getEvent() != null)
        {
            RequestContext.setExceptionPayload(new DefaultExceptionPayload(t));
        }
    }

    protected MuleMessage messageFromContextIfAvailable(MuleMessage message)
    {
        if (null != RequestContext.getEvent())
        {
            return RequestContext.getEvent().getMessage();
        }
        else
        {
            return message;
        }
    }

}

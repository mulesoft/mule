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

import org.mule.RequestContext;
import org.mule.api.ExceptionPayload;
import org.mule.api.MuleEvent;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.RoutingException;
import org.mule.api.security.SecurityException;
import org.mule.context.notification.ExceptionNotification;
import org.mule.context.notification.SecurityNotification;
import org.mule.message.DefaultExceptionPayload;
import org.mule.transport.NullPayload;

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
            if (e instanceof SecurityException)
            {
                fireNotification(new SecurityNotification((SecurityException) e, SecurityNotification.SECURITY_AUTHENTICATION_FAILED));
            }
            else
            {
                fireNotification(new ExceptionNotification(e));
            }
        }

        logException(e);
        doHandleException(e, event);
        handleTransaction(e);
        closeStream(event.getMessage());

        event.getMessage().setPayload(NullPayload.getInstance());
        ExceptionPayload exceptionPayload = new DefaultExceptionPayload(e);
        event.getMessage().setExceptionPayload(exceptionPayload);
        if (RequestContext.getEvent() != null)
        {
            RequestContext.setExceptionPayload(exceptionPayload);
        }
        return event;
    }
    
    protected void doHandleException(Exception e, MuleEvent event)
    {
        // Left this here for backwards-compatibility, remove in the next major version.
        defaultHandler(e);
        
        MessageProcessor target = null;
        if (e instanceof RoutingException)
        {
            target = ((RoutingException) e).getRoute();
        }
        routeException(event, target, e);
    }
    
    /**
     * @deprecated Override doHandleException(Exception e, MuleEvent event) instead 
     */
    // Left this here for backwards-compatibility, remove in the next major version.
    protected void defaultHandler(Throwable t)   
    {
        // empty
    }
}

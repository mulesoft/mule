/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.message.DefaultExceptionPayload;
import org.mule.transport.NullPayload;
import org.mule.util.ObjectUtils;

/**
 * <code>DefaultExceptionStrategy</code> provides a default exception handling
 * strategy.
 */

public class DefaultExceptionStrategy extends AbstractExceptionListener
{

    public void handleMessagingException(MuleMessage message, Throwable t)
    {
        defaultHandler(t);
        routeException(messageFromContextIfAvailable(message), null, t);
    }

    public void handleRoutingException(MuleMessage message, ImmutableEndpoint endpoint, Throwable t)
    {
        defaultHandler(t);
        routeException(messageFromContextIfAvailable(message), endpoint, t);
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
        markTransactionForRollback();
        // Attempt to send the exception details to an endpoint if one is set
        if (RequestContext.getEventContext() != null)
        {
            handleMessagingException(RequestContext.getEventContext().getMessage(), t);
        }
        else
        {
            logger.info("There is no current event available, routing Null message with the exception");
            handleMessagingException(new DefaultMuleMessage(NullPayload.getInstance()), t);
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

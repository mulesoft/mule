/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

import org.mule.impl.message.ExceptionPayload;
import org.mule.providers.NullPayload;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.util.ObjectUtils;

/**
 * <code>DefaultExceptionStrategy</code> provides a default exception handling
 * strategy. The class final thus to change exception handling behaviour the user
 * must reimplemented the ExceptionListener Interface
 */

public class DefaultExceptionStrategy extends AbstractExceptionListener
{
    public void handleMessagingException(UMOMessage message, Throwable t)
    {
        defaultHandler(t);
        routeException(message, null, t);
    }

    public void handleRoutingException(UMOMessage message, UMOImmutableEndpoint endpoint, Throwable t)
    {
        defaultHandler(t);
        routeException(message, endpoint, t);
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
            handleMessagingException(new MuleMessage(NullPayload.getInstance()), t);
        }
    }

    protected void defaultHandler(Throwable t)
    {
        logException(t);
        if (RequestContext.getEvent() != null)
        {
            RequestContext.setExceptionPayload(new ExceptionPayload(t));
        }
    }
}

/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.exception;

import org.mule.RequestContext;
import org.mule.api.MuleMessage;
import org.mule.api.routing.RoutingTarget;
import org.mule.message.DefaultExceptionPayload;

/**
 * Log exception, fire a notification, and clean up transaction if any.
 */
public class DefaultSystemExceptionStrategy extends AbstractExceptionListener
{
    @Override
    public void handleMessagingException(MuleMessage message, Throwable e)
    {
        if (RequestContext.getEvent() != null)
        {
            RequestContext.setExceptionPayload(new DefaultExceptionPayload(e));
        }
    }

    @Override
    public void handleRoutingException(MuleMessage message, RoutingTarget target, Throwable e)
    {
        if (RequestContext.getEvent() != null)
        {
            RequestContext.setExceptionPayload(new DefaultExceptionPayload(e));
        }
    }

    @Override
    public void handleLifecycleException(Object component, Throwable e)
    {
        // do nothing
    }

    @Override
    public void handleStandardException(Throwable e)
    {
        // do nothing
    }
}

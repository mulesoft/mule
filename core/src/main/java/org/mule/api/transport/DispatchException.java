/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.transport;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.routing.RoutingException;
import org.mule.api.routing.RoutingTarget;
import org.mule.config.i18n.Message;

/**
 * <code>DispatchException</code> is thrown when an endpoint dispatcher fails to
 * send, dispatch or receive a message.
 */
public class DispatchException extends RoutingException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -8204621943732496606L;

    /**
     * @deprecated use DispatchException(MuleEvent, RoutingTarget)
     */
    @Deprecated
    public DispatchException(MuleMessage message, RoutingTarget target)
    {
        super(message, target);
    }

    public DispatchException(MuleEvent event, RoutingTarget target)
    {
        super(event, target);
    }

    /**
     * @deprecated use DispatchException(MuleEvent, RoutingTarget, Throwable)
     */
    @Deprecated
    public DispatchException(MuleMessage message, RoutingTarget target, Throwable cause)
    {
        super(message, target, cause);
    }

    public DispatchException(MuleEvent event, RoutingTarget target, Throwable cause)
    {
        super(event, target, cause);
    }

    /**
     * @deprecated use DispatchException(Message, MuleEvent, RoutingTarget)
     */
    @Deprecated
    public DispatchException(Message message, MuleMessage muleMessage, RoutingTarget target)
    {
        super(message, muleMessage, target);
    }

    public DispatchException(Message message, MuleEvent event, RoutingTarget target)
    {
        super(message, event, target);
    }

    /**
     * @deprecated use DispatchException(Message, MuleEvent, RoutingTarget, Throwable)
     */
    @Deprecated
    public DispatchException(Message message, MuleMessage muleMessage, RoutingTarget target, Throwable cause)
    {
        super(message, muleMessage, target, cause);
    }

    public DispatchException(Message message, MuleEvent event, RoutingTarget target, Throwable cause)
    {
        super(message, event, target, cause);
    }
}

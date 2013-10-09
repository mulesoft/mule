/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.transport;

import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.RoutingException;
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

    public DispatchException(MuleEvent event, MessageProcessor target)
    {
        super(event, target);
    }

    public DispatchException(MuleEvent event, MessageProcessor target, Throwable cause)
    {
        super(event, target, cause);
    }

    public DispatchException(Message message, MuleEvent event, MessageProcessor target)
    {
        super(message, event, target);
    }

    public DispatchException(Message message, MuleEvent event, MessageProcessor target, Throwable cause)
    {
        super(message, event, target, cause);
    }
}

/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.routing;

import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.Message;

/**
 * <code>ResponseTimeoutException</code> is thrown when a response is not received
 * in a given timeout in the Response Router.
 * 
 */
public class ResponseTimeoutException extends RoutingException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 6882278747922113240L;

    public ResponseTimeoutException(Message message, MuleEvent event, MessageProcessor target)
    {
        super(message, event, target);
    }

    public ResponseTimeoutException(Message message, MuleEvent event, MessageProcessor target,
        Throwable cause)
    {
        super(message, event, target, cause);
    }
}

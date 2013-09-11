/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

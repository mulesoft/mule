/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.correlation;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.config.i18n.Message;

/**
 * TODO
 */
public class CorrelationTimeoutException  extends MessagingException
{
    /**
     * @deprecated use CorrelationTimeoutException(Message, MuleEvent)
     */
    @Deprecated
    public CorrelationTimeoutException(Message message, MuleMessage muleMessage)
    {
        super(message, muleMessage);
    }

    public CorrelationTimeoutException(Message message, MuleEvent event)
    {
        super(message, event);
    }

    /**
     * @deprecated use CorrelationTimeoutException(Message, MuleEvent, Throwable)
     */
    @Deprecated
    public CorrelationTimeoutException(Message message, MuleMessage muleMessage, Throwable cause)
    {
        super(message, muleMessage, cause);
    }

    public CorrelationTimeoutException(Message message, MuleEvent event, Throwable cause)
    {
        super(message, event, cause);
    }
}

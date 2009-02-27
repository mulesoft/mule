/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.api.MessagingException;
import org.mule.api.MuleMessage;
import org.mule.config.i18n.Message;

/**
 * TODO
 */
public class CorrelationTimeoutException  extends MessagingException
{
    public CorrelationTimeoutException(Message message, Object payload)
    {
        super(message, payload);
    }

    public CorrelationTimeoutException(Message message, Object payload, Throwable cause)
    {
        super(message, payload, cause);
    }

    public CorrelationTimeoutException(Message message, MuleMessage muleMessage)
    {
        super(message, muleMessage);
    }

    public CorrelationTimeoutException(Message message, MuleMessage muleMessage, Throwable cause)
    {
        super(message, muleMessage, cause);
    }
}

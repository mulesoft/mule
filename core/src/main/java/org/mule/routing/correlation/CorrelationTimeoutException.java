/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.correlation;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.config.i18n.Message;

public class CorrelationTimeoutException  extends MessagingException
{
    public CorrelationTimeoutException(Message message, MuleEvent event)
    {
        super(message, event);
    }

    public CorrelationTimeoutException(Message message, MuleEvent event, Throwable cause)
    {
        super(message, event, cause);
    }
}

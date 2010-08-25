/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.service;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.config.i18n.Message;

/**
 * <code>FailedToQueueEventException</code> is thrown when an event cannot be put on
 * an internal service queue.
 */

public class FailedToQueueEventException extends MessagingException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -8368283988424746098L;

    /**
     * @deprecated use FailedToQueueEventException(Message, MuleEvent)
     */
    @Deprecated
    public FailedToQueueEventException(Message message, MuleMessage muleMessage)
    {
        super(message, muleMessage);
    }

    public FailedToQueueEventException(Message message, MuleEvent event)
    {
        super(message, event);
    }

    /**
     * @deprecated use FailedToQueueEventException(Message, MuleEvent, Throwable)
     */
    @Deprecated
    public FailedToQueueEventException(Message message, MuleMessage muleMessage, Throwable cause)
    {
        super(message, muleMessage, cause);
    }

    public FailedToQueueEventException(Message message, MuleEvent event, Throwable cause)
    {
        super(message, event, cause);
    }
}

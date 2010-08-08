/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.security;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.config.i18n.Message;

/**
 * <code>SecurityException</code> is a generic security exception
 */
public abstract class SecurityException extends MessagingException
{
    /**
     * @deprecated use SecurityException(Message, MuleEvent)
     */
    @Deprecated
    protected SecurityException(Message message, MuleMessage muleMessage)
    {
        super(message, muleMessage);
    }

    protected SecurityException(Message message, MuleEvent event)
    {
        super(message, event);
    }

    /**
     * @deprecated use SecurityException(Message, MuleEvent, Throwable)
     */
    @Deprecated
    protected SecurityException(Message message, MuleMessage muleMessage, Throwable cause)
    {
        super(message, muleMessage, cause);
    }

    protected SecurityException(Message message, MuleEvent event, Throwable cause)
    {
        super(message, event, cause);
    }
}

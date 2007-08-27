/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.components.builder;

import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOMessage;

/**
 * Thrown by a MessageBuilder implementation if it cannot build the current
 * message or some other error occurs.
 */
public class MessageBuilderException extends MessagingException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1144140732378649625L;

    public MessageBuilderException(Message message, UMOMessage umoMessage)
    {
        super(message, umoMessage);
    }

    public MessageBuilderException(Message message, UMOMessage umoMessage, Throwable cause)
    {
        super(message, umoMessage, cause);
    }

    public MessageBuilderException(UMOMessage umoMessage, Throwable cause)
    {
        super(CoreMessages.failedToBuildMessage(), umoMessage, cause);
    }
}

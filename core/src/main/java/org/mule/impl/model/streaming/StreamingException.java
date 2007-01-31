/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.model.streaming;

import org.mule.config.i18n.Message;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOMessage;

/**
 * TODO
 */
public class StreamingException extends MessagingException
{

    private static final long serialVersionUID = 3346892963333693210L;

    public StreamingException(Message message, UMOMessage umoMessage)
    {
        super(message, umoMessage);
    }

    public StreamingException(Message message, UMOMessage umoMessage, Throwable cause)
    {
        super(message, umoMessage, cause);
    }
}

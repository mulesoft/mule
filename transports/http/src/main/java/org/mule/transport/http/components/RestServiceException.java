/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.components;

import org.mule.api.MessagingException;
import org.mule.api.MuleMessage;
import org.mule.config.i18n.Message;

public class RestServiceException extends MessagingException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -1026055907767407433L;

    public RestServiceException(Message message, MuleMessage muleMessage)
    {
        super(message, muleMessage);
    }

    public RestServiceException(Message message, MuleMessage muleMessage, Throwable cause)
    {
        super(message, muleMessage, cause);
    }
}

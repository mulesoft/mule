/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.components;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.config.i18n.Message;

public class RestServiceException extends MessagingException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -1026055907767407434L;

    public RestServiceException(Message message, MuleEvent event)
    {
        super(message, event);
    }

    public RestServiceException(Message message, MuleEvent event, Throwable cause)
    {
        super(message, event, cause);
    }
}

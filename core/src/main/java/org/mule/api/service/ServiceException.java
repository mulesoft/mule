/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.service;

import org.mule.api.MessagingException;
import org.mule.api.MuleMessage;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;

/**
 * <code>ServiceException</code> should be thrown when some action on a service
 * fails, such as starting or stopping
 */
// @ThreadSafe
public class ServiceException extends MessagingException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 56178344205041599L;

    private final transient Service service;

    /**
     * @param message the exception message
     */
    public ServiceException(Message message, MuleMessage muleMessage, Service service)
    {
        super(generateMessage(message, service), muleMessage);
        this.service = service;
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public ServiceException(Message message, MuleMessage muleMessage, Service service, Throwable cause)
    {
        super(generateMessage(message, service), muleMessage, cause);
        this.service = service;
    }

    public ServiceException(MuleMessage message, Service service, Throwable cause)
    {
        super(generateMessage(null, service), message, cause);
        this.service = service;
    }

    public Service getService()
    {
        return service;
    }

    private static Message generateMessage(Message previousMessage, Service service)
    {
        Message returnMessage = CoreMessages.componentCausedErrorIs(service);
        if (previousMessage != null)
        {
            previousMessage.setNextMessage(returnMessage);
            return previousMessage;
        }
        else
        {
            return returnMessage;
        }
    }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import org.mule.api.MuleException;
import org.mule.config.i18n.Message;

/**
 * Thrown by the ServerNotification Manager if unrecognised listeners or events are
 * passed to the manager
 */
public class NotificationException extends MuleException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -5998352122311445746L;

    /**
     * @param message the exception message
     */
    public NotificationException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public NotificationException(Message message, Throwable cause)
    {
        super(message, cause);
    }
}

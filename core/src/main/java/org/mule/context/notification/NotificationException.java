/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

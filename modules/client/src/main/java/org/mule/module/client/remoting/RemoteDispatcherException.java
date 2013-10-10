/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.client.remoting;

import org.mule.api.MuleException;
import org.mule.config.i18n.Message;

/**
 * Exceptions thrown by the Client RemoteDispatcher.
 */
public class RemoteDispatcherException extends MuleException
{
    /**
     * @param message the exception message
     */
    public RemoteDispatcherException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause   the exception that cause this exception to be thrown
     */
    public RemoteDispatcherException(Message message, Throwable cause)
    {
        super(message, cause);
    }
}

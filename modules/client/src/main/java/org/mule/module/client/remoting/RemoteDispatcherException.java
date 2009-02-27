/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

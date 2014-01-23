/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.consumer;

import org.mule.api.MuleEvent;
import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.Message;

/**
 * Exception thrown when a message property cannot be mapped correctly with a SOAP header.
 */
public class InvalidSoapHeaderException extends MuleRuntimeException
{
    private final MuleEvent event;

    /**
     * @param message The message for this exception.
     * @param cause The cause of the exception.
     * @param event The Mule event that is being processed.
     */
    public InvalidSoapHeaderException(Message message, Throwable cause, MuleEvent event)
    {
        super(message, cause);
        this.event = event;
    }

    public MuleEvent getEvent()
    {
        return event;
    }


}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.serialization;

import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;

/**
 * Exception to signal an error during serialization/deserialization process
 * 
 * @since 3.7.0
 */
public class SerializationException extends MuleRuntimeException
{

    private static final long serialVersionUID = -2550225226351711742L;

    public SerializationException(String message, Throwable cause)
    {
        this(MessageFactory.createStaticMessage(message), cause);
    }

    public SerializationException(String message)
    {
        this(MessageFactory.createStaticMessage(message));
    }

    public SerializationException(Message message, Throwable cause)
    {
        super(message, cause);
    }

    public SerializationException(Message message)
    {
        super(message);
    }

}

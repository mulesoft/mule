/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.exception;

import org.mule.runtime.api.connection.ConnectionException;

/**
 * Is the base {@link ConnectionException} for the Email Connector,
 *
 * @since 4.0
 */
public class EmailConnectionException extends ConnectionException
{
    /**
     * Creates a new instance with the specified detail {@code message}
     *
     * @param message the detail message
     */
    public EmailConnectionException(String message)
    {
        super(message);
    }

    /**
     * Creates a new instance with the specified detail {@code message} and {@code cause}
     *
     * @param message the detail message
     * @param cause   the exception's cause
     */
    public EmailConnectionException(String message, Throwable cause)
    {
        super(message, cause);
    }
}

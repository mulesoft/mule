/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.exception;

/**
 * This {@link EmailRetrieverException} is the base {@link RuntimeException} type for operations that
 * retrieves emails any other exceptions thrown by operations that retrieves emails will be wrapped into
 * one of this.
 *
 * @since 4.0
 */
public class EmailRetrieverException extends EmailException
{

    public static final String ERROR = "Error while retrieving emails: ";

    /**
     * Creates a new instance with the specified detail {@code message}.
     *
     * @param message the detail message
     */
    public EmailRetrieverException(String message)
    {
        super(ERROR + message);
    }

    /**
     * Creates a new instance with the {@code cause}.
     *
     * @param cause   the exception's cause
     */
    public EmailRetrieverException(Throwable cause)
    {
        super(ERROR + cause.getMessage(), cause);
    }

    /**
     * Creates a new instance with the specified detail {@code message} and {@code cause}
     *
     * @param message the detail message
     * @param cause   the exception's cause
     */
    public EmailRetrieverException(String message, Throwable cause)
    {
        super(ERROR + message, cause);
    }
}

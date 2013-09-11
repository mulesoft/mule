/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.endpoint;

import org.mule.api.MuleException;
import org.mule.config.i18n.Message;

/**
 * <code>EndpointException</code> is an abstract exception extended by endpoint
 * specific exceptions.
 */

public class EndpointException extends MuleException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 3219403251233216800L;

    /**
     * @param message the exception message
     */
    public EndpointException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public EndpointException(Message message, Throwable cause)
    {
        super(message, cause);
    }

    public EndpointException(Throwable cause)
    {
        super(cause);
    }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.service;

import org.mule.api.endpoint.EndpointException;
import org.mule.config.i18n.Message;

/**
 * <code>TransportFactoryException</code> is thrown by the endpoint factory if the
 * endpoint service cannot be found in the META-INF/services directory or if any part
 * of the endpoint cannot be instanciated.
 * 
 */

public class TransportFactoryException extends EndpointException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -4166766364690837213L;

    /**
     * @param message the exception message
     */
    public TransportFactoryException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public TransportFactoryException(Message message, Throwable cause)
    {
        super(message, cause);
    }

    public TransportFactoryException(Throwable cause)
    {
        super(cause);
    }
}

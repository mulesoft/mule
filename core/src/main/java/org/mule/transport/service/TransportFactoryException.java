/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

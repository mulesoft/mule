/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.service;

import org.mule.config.i18n.Message;

/**
 * <code>TransportServiceException</code> is thrown if a ProviderServicedescriptor
 * has a service error set. This is usually because the endpoint/connector cannot be
 * created from a service descriptor
 * 
 */

public class TransportServiceException extends TransportFactoryException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 5377271625492627661L;

    /**
     * @param message the exception message
     */
    public TransportServiceException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public TransportServiceException(Message message, Throwable cause)
    {
        super(message, cause);
    }
}

/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.service;

import org.mule.config.i18n.MessageFactory;

/**
 * <code>TransportServiceNotFoundException</code> is thorown if no matching service
 * endpoint descriptor is found for the connector protocol.
 * 
 */

public class TransportServiceNotFoundException extends TransportFactoryException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -8321406750213654479L;

    /**
     * @param location the path of the service
     */
    public TransportServiceNotFoundException(String location)
    {
        super(MessageFactory.createStaticMessage(location));
    }

    /**
     * @param location the path of the service
     * @param cause the exception that cause this exception to be thrown
     */
    public TransportServiceNotFoundException(String location, Throwable cause)
    {
        super(MessageFactory.createStaticMessage(location), cause);
    }
}

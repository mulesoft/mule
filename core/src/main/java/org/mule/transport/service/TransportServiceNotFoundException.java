/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

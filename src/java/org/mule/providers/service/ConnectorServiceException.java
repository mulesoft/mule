/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.service;

/**
 * <code>ConnectorServiceException</code> is thrown if a ProviderServicedescriptor has
 * a service error set. This is usually because the endpoint/connector cannot be created from
 * a service descriptor
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class ConnectorServiceException extends ConnectorFactoryException
{
    public ConnectorServiceException(String message)
    {
        super(message);
    }

    public ConnectorServiceException(String message, Throwable cause)
    {
        super(message, cause);
    }
}

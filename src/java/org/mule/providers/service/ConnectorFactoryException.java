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

import org.mule.umo.endpoint.EndpointException;

/**
 * <code>ConnectorFactoryException</code> is thrown by the endpoint factory if the endpoint
 * service cannot be found in the META-INF/services directory or if any part of the endpoint
 * cannot be instanciated.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class ConnectorFactoryException extends EndpointException
{
    public ConnectorFactoryException(String message)
    {
        super(message);
    }

    public ConnectorFactoryException(String message, Throwable cause)
    {
        super(message, cause);
    }
}

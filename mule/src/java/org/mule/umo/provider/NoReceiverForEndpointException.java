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
package org.mule.umo.provider;

import org.mule.umo.endpoint.EndpointException;

/**
 * <code>NoReceiverForEndpointException</code> is thrown when an enpoint is specified for a
 * receiver but no such receiver exists.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class NoReceiverForEndpointException extends EndpointException
{
    public NoReceiverForEndpointException(String message)
    {
        super(message);
    }

    public NoReceiverForEndpointException(String message, Throwable cause)
    {
        super(message, cause);
    }

}

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
 * <code>DispatchException</code> is thrown when an endpoint dispatcher fails to
 * send, dispatch or receive a message.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class DispatchException extends EndpointException
{
    public DispatchException(String message)
    {
        super(message);
    }

    public DispatchException(String message, Throwable cause)
    {
        super(message, cause);
    }
}

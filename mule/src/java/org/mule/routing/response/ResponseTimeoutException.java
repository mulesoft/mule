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
package org.mule.routing.response;

import org.mule.umo.routing.RoutingException;

/**
 * <code>ResponseTimeoutException</code> is thrown when a response is not received
 * in a given timeout in the Response Router.
 *
 * @see org.mule.umo.routing.UMOResponseRouter
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class ResponseTimeoutException extends RoutingException
{
    public ResponseTimeoutException(String message)
    {
        super(message);
    }

    public ResponseTimeoutException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public ResponseTimeoutException(String message, Throwable cause, Object event)
    {
        super(message, cause, event);
    }
}

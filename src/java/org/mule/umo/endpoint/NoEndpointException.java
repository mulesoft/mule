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
package org.mule.umo.endpoint;



/**
 * <code>NoEndpointException</code> is thrown when an event is dispatched, but there is no
 * enpoint for it.  Within Mule this rarely occurs, but external applications triggering events
 * into Mule may no behave as well :)
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class NoEndpointException extends EndpointException
{
    public NoEndpointException(String message)
    {
        super(message);
    }

    public NoEndpointException(String message, Throwable cause)
    {
        super(message, cause);
    }

}

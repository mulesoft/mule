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
 * <code>EndpointNotFoundException</code> is thrown when an endpoint name or protocol is
 * specified but a matching endpoint is not registered with the Mule server
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class EndpointNotFoundException extends EndpointException
{
    public EndpointNotFoundException(String message)
    {
        super(message);
    }

    public EndpointNotFoundException(String message, Throwable cause)
    {
        super(message, cause);
    }

}

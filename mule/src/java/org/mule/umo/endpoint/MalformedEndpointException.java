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
 * <code>MalformedEndpointException</code> is thrown by the MuleEndpointURI class if it fails to parse a Url
 * @see org.mule.impl.endpoint.MuleEndpointURI
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class MalformedEndpointException extends EndpointException
{
    /**
     * @param message the exception message
     */
    public MalformedEndpointException(String message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause   the exception that cause this exception to be thrown
     */
    public MalformedEndpointException(String message, Throwable cause)
    {
        super(message, cause);
    }
}

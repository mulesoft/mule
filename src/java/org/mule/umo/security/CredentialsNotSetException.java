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
package org.mule.umo.security;

import org.mule.umo.endpoint.UMOImmutableEndpoint;

/**
 * <code>CredentialsNotSetException</code> is thrown when user credentials
 * cannot be obtained from the current message
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class CredentialsNotSetException extends UnauthorisedException
{
    /**
     * @param message the exception message
     */
    public CredentialsNotSetException(String message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause   the exception that cause this exception to be thrown
     */
    public CredentialsNotSetException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public CredentialsNotSetException(UMOSecurityContext context, UMOImmutableEndpoint endpoint, UMOEndpointSecurityFilter filter)
    {
        super(context, endpoint, filter);
    }
}

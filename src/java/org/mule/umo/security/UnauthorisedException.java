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

import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

/**
 * <code>UnauthorisedException</code> is thrown if authentication fails
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class UnauthorisedException extends UMOSecurityException
{
    /**
     * @param message the exception message
     */
    public UnauthorisedException(String message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause   the exception that cause this exception to be thrown
     */
    public UnauthorisedException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public UnauthorisedException(UMOSecurityContext context, UMOImmutableEndpoint endpoint, UMOEndpointSecurityFilter filter)
    {
        super(constructMessage(context, endpoint, filter));
    }

    private static String constructMessage(UMOSecurityContext context, UMOImmutableEndpoint endpoint, UMOEndpointSecurityFilter filter) {

        StringBuffer buf = new StringBuffer();
        if(context==null) {
            buf.append("Regired authentication is set on ").append(filter.getClass().getName());
            buf.append(" but there was no security context on the session.");
        } else {
            buf.append("User authentication failed for: " + context.getAuthentication().getPrincipal());
        }
        buf.append("Failed to ");
        buf.append((endpoint.getType().equals(UMOEndpoint.ENDPOINT_TYPE_RECEIVER) ? "receive" : "dispatch"));
        buf.append(" via endpoint ").append(endpoint.getEndpointURI());

        return buf.toString();
    }
}

/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ssl;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;

public class TlsConnectorFunctionalTestCase extends SslConnectorFunctionalTestCase
{
    private int port = 61665;

    protected UMOEndpointURI getInDest()
    {
        try
        {
            logger.debug("Using port " + port);
            return new MuleEndpointURI("tls://localhost:" + port);
        }
        catch (EndpointException e)
        {
            fail(e.getMessage());
            return null;
        }
    }

    protected UMOEndpointURI getOutDest()
    {
        return null;
    }

}

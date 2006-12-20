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
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TlsConnectorFunctionalTestCase extends SslConnectorFunctionalTestCase
{
    /**
     * logger used by this class
     */
    protected static Log logger = LogFactory.getLog(TlsConnectorFunctionalTestCase.class);

    private int port = 61655;

    protected UMOEndpointURI getInDest()
    {
        try
        {
            return new MuleEndpointURI("tls://localhost:" + port);
        }
        catch (MalformedEndpointException e)
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

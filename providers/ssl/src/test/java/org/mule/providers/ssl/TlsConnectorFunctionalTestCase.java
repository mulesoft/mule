/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.ssl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOConnector;

import javax.net.ssl.SSLSocketFactory;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class TlsConnectorFunctionalTestCase extends SslConnectorFunctionalTestCase
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(TlsConnectorFunctionalTestCase.class);

    private int port = 61655;

    protected UMOEndpointURI getInDest()
    {
        try {
            return new MuleEndpointURI("tls://localhost:" + port);
        } catch (MalformedEndpointException e) {
            fail(e.getMessage());
            return null;
        }
    }

    protected UMOEndpointURI getOutDest()
    {
        return null;
    }

    public UMOConnector createConnector() throws Exception
    {
        SslConnector cnn = SslConnectorTestCase.createConnector(false);
        return cnn;
    }

    protected Socket createSocket(URI uri) throws IOException
    {
        return SSLSocketFactory.getDefault().createSocket(uri.getHost(), uri.getPort());
    }
}

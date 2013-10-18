/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.endpoint;

import org.mule.api.endpoint.MalformedEndpointException;

import java.net.URI;
import java.util.Properties;

/**
 * <code>SocketEndpointBuilder</code> builds an endpointUri based on host and port
 * only
 */
public class SocketEndpointURIBuilder extends AbstractEndpointURIBuilder
{
    protected void setEndpoint(URI uri, Properties props) throws MalformedEndpointException
    {
        // set the endpointUri to be a proper url if host and port are set
        if (uri.getPort() == -1)
        {
            // try the form tcp://6666
            try
            {
                int port = Integer.parseInt(uri.getHost());
                this.address = uri.getScheme() + "://localhost:" + port;
            }
            catch (NumberFormatException e)
            {
                // ignore
            }
        }

        if (address == null)
        {
            this.address = uri.getScheme() + "://" + uri.getHost();
            if (uri.getPort() != -1)
            {
                this.address += ":" + uri.getPort();
            }
        }
    }
}

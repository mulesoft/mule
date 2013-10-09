/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

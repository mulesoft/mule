/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp;

import org.mule.umo.endpoint.UMOImmutableEndpoint;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * This is used to adapt an endpoint so that it can be used as a key for sockets.  It must
 * meet two requirements: (1) implement hash and equals in a way that reflects socket identity
 * (ie using address and port); (2) allow access to the endpoint for use in the socket factory.
 * For simplicity we also expose the connector, address and port directly.
 */
public class TcpSocketKey
{

    private InetSocketAddress address;
    private UMOImmutableEndpoint endpoint;

    public TcpSocketKey(UMOImmutableEndpoint endpoint)
    {
        if (!(endpoint.getConnector() instanceof TcpConnector))
        {
            throw new IllegalArgumentException("Sockets must be keyed via a TCP endpoint");
        }
        this.endpoint = endpoint;
        address = new InetSocketAddress(
                endpoint.getEndpointURI().getHost(),
                endpoint.getEndpointURI().getPort());
    }

    public boolean equals(Object obj)
    {
        return obj instanceof TcpSocketKey && address.equals(((TcpSocketKey) obj).address);
    }

    public int hashCode()
    {
        return address.hashCode();
    }

    public UMOImmutableEndpoint getEndpoint()
    {
        return endpoint;
    }

    public TcpConnector getConnector()
    {
        return (TcpConnector) endpoint.getConnector();
    }

    public InetAddress getInetAddress()
    {
        return address.getAddress();
    }

    public int getPort()
    {
        return address.getPort();
    }

    public String toString()
    {
        return getInetAddress() + ":" + getPort();
    }

}

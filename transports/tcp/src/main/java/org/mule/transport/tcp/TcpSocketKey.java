/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp;

import org.mule.api.endpoint.ImmutableEndpoint;

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
    private ImmutableEndpoint endpoint;

    public TcpSocketKey(ImmutableEndpoint endpoint)
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

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof TcpSocketKey && address.equals(((TcpSocketKey) obj).address);
    }

    @Override
    public int hashCode()
    {
        return address.hashCode();
    }

    public ImmutableEndpoint getEndpoint()
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

    @Override
    public String toString()
    {
        return getInetAddress() + ":" + getPort();
    }

}

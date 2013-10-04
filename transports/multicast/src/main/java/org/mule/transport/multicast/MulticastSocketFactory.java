/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.multicast;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transport.Connector;
import org.mule.transport.udp.UdpSocketFactory;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * TODO
 */
public class MulticastSocketFactory extends UdpSocketFactory
{

    public Object makeObject(Object key) throws Exception
    {
        ImmutableEndpoint ep = (ImmutableEndpoint)key;
        MulticastSocket socket = (MulticastSocket)super.makeObject(key);
        socket.setLoopbackMode(((MulticastConnector)ep.getConnector()).isLoopback());
        int ttl  = ((MulticastConnector)ep.getConnector()).getTimeToLive();
        if(ttl!= Connector.INT_VALUE_NOT_SET)
        {
            socket.setTimeToLive(ttl);
        }
        return socket;
    }


    @java.lang.Override
    public void destroyObject(Object key, Object object) throws Exception
    {
        ImmutableEndpoint ep = (ImmutableEndpoint)key;
        InetAddress inetAddress;
        String host = ep.getEndpointURI().getHost();
        if("null".equalsIgnoreCase(host))
        {
            inetAddress = InetAddress.getLocalHost();
        }
        else
        {
            inetAddress = InetAddress.getByName(host);
        }
        MulticastSocket socket = (MulticastSocket)object;
        socket.leaveGroup(inetAddress);
        super.destroyObject(key, object);
    }

    protected DatagramSocket createSocket() throws IOException
    {
        return new MulticastSocket();
    }

    protected DatagramSocket createSocket(int port) throws IOException
    {
        throw new IllegalArgumentException("A group host or IP address is required");
    }

    protected DatagramSocket createSocket(int port, InetAddress inetAddress) throws IOException
    {
        MulticastSocket socket = new MulticastSocket(port);
        socket.joinGroup(inetAddress);
        return socket;
    }
}

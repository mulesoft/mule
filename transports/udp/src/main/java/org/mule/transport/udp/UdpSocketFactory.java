/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.udp;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transport.Connector;
import org.mule.util.MapUtils;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.KeyedPoolableObjectFactory;

/**
 * Creates a client socket using the host and port address supplied in the endpoint URI.  Addtional
 * socket parameters will also be set from the connector
 */
public class UdpSocketFactory implements KeyedPoolableObjectFactory
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(UdpSocketFactory.class);

    public Object makeObject(Object key) throws Exception
    {
        ImmutableEndpoint ep = (ImmutableEndpoint)key;
        DatagramSocket socket;

        if(ep instanceof InboundEndpoint)
        {
            int port = ep.getEndpointURI().getPort();
            String host = ep.getEndpointURI().getHost();
            if(port > 0)
            {
                if("null".equalsIgnoreCase(host))
                {
                    socket = createSocket(port);
                }
                else
                {
                    socket = createSocket(port, InetAddress.getByName(host));
                }
            }
            else
            {
                socket = createSocket();
            }
        }
        else
        {
            //If this is a client socket create a default instance
            socket = createSocket();
        }

        UdpConnector connector = (UdpConnector)ep.getConnector();
        //There is some overhead in stting socket timeout and buffer size, so we're
        //careful here only to set if needed
        if (connector.getReceiveBufferSize() != Connector.INT_VALUE_NOT_SET
            && socket.getReceiveBufferSize() != connector.getReceiveBufferSize())
        {
            socket.setReceiveBufferSize(connector.getReceiveBufferSize());
        }
        if (connector.getSendBufferSize() != Connector.INT_VALUE_NOT_SET
            && socket.getSendBufferSize() != connector.getSendBufferSize())
        {
            socket.setSendBufferSize(connector.getSendBufferSize());
        }
        if (connector.getTimeout() != Connector.INT_VALUE_NOT_SET
            && socket.getSoTimeout() != connector.getTimeout())
        {
            socket.setSoTimeout(connector.getTimeout());
        }
        socket.setBroadcast(connector.isBroadcast());
        return socket;
    }

    public void destroyObject(Object key, Object object) throws Exception
    {
        Socket socket = (Socket)object;
        if(!socket.isClosed())
        {
            socket.close();
        }
    }

    public boolean validateObject(Object key, Object object)
    {
        DatagramSocket socket = (DatagramSocket)object;
        return !socket.isClosed();
    }

    public void activateObject(Object key, Object object) throws Exception
    {
        // nothing to do        
    }

    public void passivateObject(Object key, Object object) throws Exception
    {
        ImmutableEndpoint ep = (ImmutableEndpoint)key;

        boolean keepSocketOpen = MapUtils.getBooleanValue(ep.getProperties(),
            UdpConnector.KEEP_SEND_SOCKET_OPEN_PROPERTY, ((UdpConnector)ep.getConnector()).isKeepSendSocketOpen());
        DatagramSocket socket = (DatagramSocket)object;

        if (!keepSocketOpen)
        {
            if (socket != null)
            {
                socket.close();
            }
        }
    }

    protected DatagramSocket createSocket() throws IOException
    {
        return new DatagramSocket();
    }

    protected DatagramSocket createSocket(int port) throws IOException
    {
        return new DatagramSocket(port);
    }

    protected DatagramSocket createSocket(int port, InetAddress inetAddress) throws IOException
    {
        return new DatagramSocket(port, inetAddress);
    }
}

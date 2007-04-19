/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.tcp;

import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOConnector;
import org.mule.util.MapUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Creates a client socket using the host and port address supplied in the endpoint URI.  Addtional
 * socket parameters will also be set from the connector
 */
public class TcpSocketFactory implements PooledSocketFactory
{
    /**
     * logger used by this class
     */
    private static final Log logger = LogFactory.getLog(TcpSocketFactory.class);

    public Object makeObject(Object key) throws Exception
    {
        UMOImmutableEndpoint ep = (UMOImmutableEndpoint) key;
        int port = ep.getEndpointURI().getPort();
        InetAddress inetAddress = InetAddress.getByName(ep.getEndpointURI().getHost());
        Socket socket = createSocket(port, inetAddress);
        socket.setReuseAddress(true);

        TcpConnector connector = (TcpConnector)ep.getConnector();
        //There is some overhead in stting socket timeout and buffer size, so we're
        //careful here only to set if needed
        if (connector.getReceiveBufferSize() != UMOConnector.INT_VALUE_NOT_SET
            && socket.getReceiveBufferSize() != connector.getReceiveBufferSize())
        {
            socket.setReceiveBufferSize(connector.getReceiveBufferSize());
        }
        if (connector.getSendBufferSize() != UMOConnector.INT_VALUE_NOT_SET
            && socket.getSendBufferSize() != connector.getSendBufferSize())
        {
            socket.setSendBufferSize(connector.getSendBufferSize());
        }
        if (connector.getReceiveTimeout() != UMOConnector.INT_VALUE_NOT_SET
            && socket.getSoTimeout() != connector.getReceiveTimeout())
        {
            socket.setSoTimeout(connector.getSendTimeout());
        }
        socket.setTcpNoDelay(connector.isSendTcpNoDelay());
        socket.setKeepAlive(connector.isKeepAlive());
        return socket;
    }

    protected Socket createSocket(int port, InetAddress inetAddress) throws IOException
    {
        return new Socket(inetAddress, port);
    }

    public void destroyObject(Object key, Object object) throws Exception
    {
        Socket socket = (Socket) object;
        if(!socket.isClosed())
        {
            socket.close();
        }
    }

    public boolean validateObject(Object key, Object object)
    {
        Socket socket = (Socket) object;
        return !socket.isClosed();       
    }

    public void activateObject(Object key, Object object) throws Exception
    {
        // cannot really activate a Socket
    }

    public void passivateObject(Object key, Object object) throws Exception
    {
        UMOImmutableEndpoint ep = (UMOImmutableEndpoint) key;

        boolean keepSocketOpen = MapUtils.getBooleanValue(ep.getProperties(),
            TcpConnector.KEEP_SEND_SOCKET_OPEN_PROPERTY, ((TcpConnector) ep.getConnector()).isKeepSendSocketOpen());
        Socket socket = (Socket) object;

        if (!keepSocketOpen)
        {
            try
            {
                if (socket != null)
                {
                    socket.close();
                }
            }
            catch (IOException e)
            {
                logger.debug("Failed to close socket after dispatch: " + e.getMessage());
            }
        }
    }
    
}

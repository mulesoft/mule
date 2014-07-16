/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.udp;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.transport.AbstractConnector;

import java.net.DatagramSocket;

import org.apache.commons.pool.impl.GenericKeyedObjectPool;

/**
 * <code>UdpConnector</code> can send and receive Mule events as Datagram packets.
 */
public class UdpConnector extends AbstractConnector
{
    public static final String UDP = "udp";
    public static final int DEFAULT_SOCKET_TIMEOUT = INT_VALUE_NOT_SET;
    public static final int DEFAULT_BUFFER_SIZE = 1024 * 16;
    public static final String KEEP_SEND_SOCKET_OPEN_PROPERTY = "keepSendSocketOpen";
    public static final String ADDRESS_PROPERTY = "packet.address";
    public static final String PORT_PROPERTY = "packet.port";

    protected int timeout = DEFAULT_SOCKET_TIMEOUT;
    protected int sendBufferSize = DEFAULT_BUFFER_SIZE;
    protected int receiveBufferSize = DEFAULT_BUFFER_SIZE;
    protected boolean keepSendSocketOpen = true;
    protected boolean broadcast;
    protected GenericKeyedObjectPool dispatcherSocketsPool = new GenericKeyedObjectPool();
    protected UdpSocketFactory socketFactory;

    public UdpConnector(MuleContext context)
    {
        super(context);
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        socketFactory = new UdpSocketFactory();
        dispatcherSocketsPool.setFactory(socketFactory);
        dispatcherSocketsPool.setTestOnBorrow(true);
        dispatcherSocketsPool.setTestOnReturn(true);
        //There should only be one pooled instance per socket (key)
        dispatcherSocketsPool.setMaxActive(1);
    }

    @Override
    protected void doDispose()
    {
        try
        {
            dispatcherSocketsPool.close();
        }
        catch (Exception e)
        {
            logger.warn("Failed to close dispatcher socket pool: " + e.getMessage());
        }
    }

    @Override
    protected void doConnect() throws Exception
    {
        // template method
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        dispatcherSocketsPool.clear();
    }

    @Override
    protected void doStart() throws MuleException
    {
        // template method
    }

    @Override
    protected void doStop() throws MuleException
    {
        // template method
    }

    public String getProtocol()
    {
        return UDP;
    }

    public int getTimeout()
    {
        return this.timeout;
    }

    public void setTimeout(int timeout)
    {
        if (timeout < 0)
        {
            timeout = DEFAULT_SOCKET_TIMEOUT;
        }
        this.timeout = timeout;
    }

    public int getSendBufferSize()
    {
        return sendBufferSize;
    }

    public void setSendBufferSize(int sendBufferSize)
    {
        if (sendBufferSize < 1)
        {
            sendBufferSize = DEFAULT_BUFFER_SIZE;
        }
        this.sendBufferSize = sendBufferSize;
    }

    public int getReceiveBufferSize()
    {
        return receiveBufferSize;
    }

    public void setReceiveBufferSize(int receiveBufferSize)
    {
        if (receiveBufferSize < 1)
        {
            receiveBufferSize = DEFAULT_BUFFER_SIZE;
        }
        this.receiveBufferSize = receiveBufferSize;
    }

    public boolean isBroadcast()
    {
        return broadcast;
    }

    public void setBroadcast(boolean broadcast)
    {
        this.broadcast = broadcast;
    }


    public boolean isKeepSendSocketOpen()
    {
        return keepSendSocketOpen;
    }

    public void setKeepSendSocketOpen(boolean keepSendSocketOpen)
    {
        this.keepSendSocketOpen = keepSendSocketOpen;
    }

    /**
     * Lookup a socket in the list of dispatcher sockets but don't create a new
     * socket
     *
     * @param endpoint
     */
    DatagramSocket getSocket(ImmutableEndpoint endpoint) throws Exception
    {
        return (DatagramSocket) dispatcherSocketsPool.borrowObject(endpoint);
    }

    DatagramSocket getServerSocket(ImmutableEndpoint endpoint) throws Exception
    {
        return (DatagramSocket) socketFactory.makeObject(endpoint);
    }

    void releaseSocket(DatagramSocket socket, ImmutableEndpoint endpoint) throws Exception
    {
        // Sockets can't be recycled if we close them at the end...
        if (!keepSendSocketOpen)
        {
            dispatcherSocketsPool.clear(endpoint);
        }
        else
        {
            dispatcherSocketsPool.returnObject(endpoint, socket);
        }
    }

    @Override
    protected Object getReceiverKey(FlowConstruct flowConstruct, InboundEndpoint endpoint)
    {
        return endpoint.getEndpointURI().getAddress() + "/" + flowConstruct.getName();
    }
}

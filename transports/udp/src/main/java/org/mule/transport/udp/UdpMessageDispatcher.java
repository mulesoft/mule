/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.udp;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.retry.RetryContext;
import org.mule.transport.AbstractMessageDispatcher;
import org.mule.transport.NullPayload;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * <code>UdpMessageDispatcher</code> is responsible for dispatching MuleEvents as
 * UDP packets on the network
 */

public class UdpMessageDispatcher extends AbstractMessageDispatcher
{
    protected final UdpConnector connector;

    public UdpMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (UdpConnector)endpoint.getConnector();
    }

    @Override
    public RetryContext validateConnection(RetryContext retryContext)
    {
        DatagramSocket socket = null;
        try
        {
            socket = connector.getSocket(endpoint);

            retryContext.setOk();
        }
        catch (Exception ex)
        {
            retryContext.setFailed(ex);
        }
        finally
        {
            if (socket != null)
            {
                try
                {
                    connector.releaseSocket(socket, endpoint);
                }
                catch (Exception e)
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Failed to release a socket " + socket, e);
                    }
                }
            }
        }

        return retryContext;

    }

    protected void doConnect() throws Exception
    {
        // nothing, there is an optional validation in validateConnection()
        
    }

    protected void doDisconnect() throws Exception
    {
        // nothing to do
    }


    protected synchronized void doDispatch(MuleEvent event) throws Exception
    {
        ImmutableEndpoint ep = event.getEndpoint();

        DatagramSocket socket = connector.getSocket(ep);
        try
        {
            byte[] payload = event.transformMessageToBytes();

            int port = ep.getEndpointURI().getPort();
            InetAddress inetAddress = null;
            //TODO, check how expensive this operation is
            if("null".equalsIgnoreCase(ep.getEndpointURI().getHost()))
            {
                inetAddress = InetAddress.getLocalHost();
            }
            else
            {
                inetAddress = InetAddress.getByName(ep.getEndpointURI().getHost());
            }

            write(socket, payload, port, inetAddress);
        }
        finally
        {
            connector.releaseSocket(socket, ep);
        }
    }

    protected void write(DatagramSocket socket, byte[] data, int port, InetAddress inetAddress) throws IOException
    {
        DatagramPacket packet = new DatagramPacket(data, data.length);
        if (port >= 0)
        {
            packet.setPort(port);
        }
        packet.setAddress(inetAddress);
        socket.send(packet);
    }

    protected synchronized MuleMessage doSend(MuleEvent event) throws Exception
    {
        doDispatch(event);
        // If we're doing sync receive try and read return info from socket
        if (event.getEndpoint().isSynchronous())
        {
            DatagramSocket socket = connector.getSocket(event.getEndpoint());
            DatagramPacket result = receive(socket, event.getTimeout());
            if (result == null)
            {
                return new DefaultMuleMessage(NullPayload.getInstance(), connector.getMuleContext());
            }
            return new DefaultMuleMessage(connector.getMessageAdapter(result), event.getMessage(), connector.getMuleContext());
        }
        else
        {
            return new DefaultMuleMessage(NullPayload.getInstance(), connector.getMuleContext());
        }
    }

    private DatagramPacket receive(DatagramSocket socket, int timeout) throws IOException
    {
        int origTimeout = socket.getSoTimeout();
        try
        {
            DatagramPacket packet = new DatagramPacket(new byte[connector.getReceiveBufferSize()],
                connector.getReceiveBufferSize());

            if(timeout > 0 && timeout != socket.getSoTimeout())
            {
                socket.setSoTimeout(timeout);
            }
            socket.receive(packet);
            return packet;
        }
        finally
        {
            if(socket.getSoTimeout()!= origTimeout)
            {
                socket.setSoTimeout(origTimeout);
            }
        }
    }

    protected void doDispose()
    {
        // template method
    }
}

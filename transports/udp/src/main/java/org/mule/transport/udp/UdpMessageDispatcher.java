/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.udp;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.retry.RetryContext;
import org.mule.api.transformer.DataType;
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

    @Override
    protected void doConnect() throws Exception
    {
        // nothing, there is an optional validation in validateConnection()
        
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        // nothing to do
    }


    @Override
    protected synchronized void doDispatch(MuleEvent event) throws Exception
    {
        DatagramSocket socket = connector.getSocket(endpoint);
        try
        {
            byte[] payload = event.transformMessage(DataType.BYTE_ARRAY_DATA_TYPE);

            int port = endpoint.getEndpointURI().getPort();
            InetAddress inetAddress = null;
            //TODO, check how expensive this operation is
            if("null".equalsIgnoreCase(endpoint.getEndpointURI().getHost()))
            {
                inetAddress = InetAddress.getLocalHost();
            }
            else
            {
                inetAddress = InetAddress.getByName(endpoint.getEndpointURI().getHost());
            }

            write(socket, payload, port, inetAddress);
        }
        finally
        {
            connector.releaseSocket(socket, endpoint);
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

    @Override
    protected synchronized MuleMessage doSend(MuleEvent event) throws Exception
    {
        doDispatch(event);
        // If we're doing sync receive try and read return info from socket
        if (endpoint.getExchangePattern().hasResponse())
        {
            DatagramSocket socket = connector.getSocket(endpoint);
            DatagramPacket result = receive(socket, event.getTimeout());
            if (result == null)
            {
                return createNullMuleMessage();
            }
            return createMuleMessage(result, event.getMessage(), endpoint.getEncoding());
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

    @Override
    protected void doDispose()
    {
        // template method
    }
}

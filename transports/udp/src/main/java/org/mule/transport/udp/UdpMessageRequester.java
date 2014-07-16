/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.udp;

import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.retry.RetryContext;
import org.mule.transport.AbstractMessageRequester;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Responsible for requesting MuleEvents as UDP packets on the network
 */

public class UdpMessageRequester extends AbstractMessageRequester
{
    
    protected final UdpConnector connector;

    public UdpMessageRequester(InboundEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (UdpConnector)endpoint.getConnector();
    }

    @Override
    protected void doConnect() throws Exception
    {
        // nothing, there is an optional validation in validateConnection()
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
    protected void doDisconnect() throws Exception
    {
        // nothing to do
    }

    private DatagramPacket request(DatagramSocket socket, int timeout) throws IOException
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

    /**
     * Make a specific request to the underlying transport
     *
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a MuleMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    @Override
    protected MuleMessage doRequest(long timeout) throws Exception
    {
        DatagramSocket socket = connector.getSocket(endpoint);
        DatagramPacket result = request(socket, (int)timeout);
        if (result == null)
        {
            return null;
        }
        return createMuleMessage(result, endpoint.getEncoding());
    }

    @Override
    protected void doDispose()
    {
        // template method
    }

}

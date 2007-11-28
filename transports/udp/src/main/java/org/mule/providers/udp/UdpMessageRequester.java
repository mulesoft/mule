/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.udp;

import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageRequester;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Map;

/**
 * Responsible for requesting MuleEvents as UDP packets on the network
 */

public class UdpMessageRequester extends AbstractMessageRequester
{
    
    protected final UdpConnector connector;

    public UdpMessageRequester(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (UdpConnector)endpoint.getConnector();
    }

    protected void doConnect() throws Exception
    {
        // Test the connection
        DatagramSocket socket = connector.getSocket(endpoint);
        connector.releaseSocket(socket, endpoint);
    }

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
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected UMOMessage doRequest(long timeout) throws Exception
    {
        DatagramSocket socket = connector.getSocket(endpoint);
        DatagramPacket result = request(socket, (int)timeout);
        if (result == null)
        {
            return null;
        }
        return new MuleMessage(connector.getMessageAdapter(result), (Map)null);
    }

    protected void doDispose()
    {
        // template method
    }

}